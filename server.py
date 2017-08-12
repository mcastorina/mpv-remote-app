#!/usr/bin/python3
# FIXME: make a proper state machine

import os
import sys
import json
import time
import hmac
import signal
import socket
import select
import argparse
import threading
import subprocess
from collections import deque

# Unix socket used to communicate with mpv
# mpv needs to be started with the flag --input-ipc-server mpv_socket
mpv_socket = "/tmp/mpvsocket"

# Base address for file browsing
ROOT_DIR = None
# Do not send hidden filenames
no_hidden = False
# Filetype filter (empty for all files)
filetypes = []

# UDP socket to communicate with app
sock = None
# Password for the server
server_password = None
# History of commands
history = deque()
# Max size of history
HISTORY_SIZE = 32

# Whitelist for commands (besides get / set / show property)
COMMAND_WHITELIST = ["seek", "show_text", "cycle pause", "quit"]


# Usage message
help_string = (
    "Listens on PORT for UDP commands to send to mpv."
    " PASSWORD sets the password of this server and must be used"
    " by the client to alter the state of mpv."
    " Note: mpv must be started with the --input-ipc-server flag."
)

# Signal handler for SIGINT
def cleanup(signal, frame):
    global sock
    sock.close()
    sys.exit(0)

# Run command and return (exit code, stdout)
def call(args):
    child = subprocess.Popen(' '.join(args), shell=True,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = child.communicate()
    ret = child.poll()
    return (ret, stdout.decode().strip())

# Run "mpv --no-terminal --input-ipc-server sock path" in the background
def play(path, sock=mpv_socket):
    args = [
        "mpv",
        "--no-terminal",
        "--input-ipc-server", sock,
        path, "&"
    ]
    os.system(" ".join(args))

# Send message to mpv (on sock) via socat
def socat(command, sock=mpv_socket):
    ret, out = call(["echo", "'%s'" % command, '|', "socat", "-", sock])
    return out

# Get the property from the mpv listening on sock
def get_property(property, sock=mpv_socket):
    try:
        cmd = json.dumps({"command": ["get_property", property]})
        out = json.loads(socat(cmd, sock))
        if out["error"] != "success":
            return None
        return out["data"]
    except: return None

# Set the property on the mpv listening on sock
def set_property(property, value, sock=mpv_socket):
    try:
        cmd = json.dumps({"command": ["set_property", property, value]})
        out = json.loads(socat(cmd, sock))
        if out["error"] != "success":
            return False
        return True
    except: return False

# Show the property (on OSD) on the mpv listening on sock
def show_property(property, pre=None, post="", sock=mpv_socket):
    try:
        if pre is None:
            pre = property.title() + ": "
        arg = "\"%s${%s}%s\"" % (pre, property, post)
        return send_command("show_text", [arg], sock)
    except: return None

# Sends command to mpv listening on sock
def send_command(command, args, sock=mpv_socket):
    try:
        args = [str(arg) for arg in args]
        cmd = "%s %s" % (command, ' '.join(args))
        return socat(cmd)
    except: return False

# Waits delay (ms) on sock and returns (data, addr) or None
# If delay is None, then this function will block
def recv(delay=None):
    global sock
    global server_password

    sock.setblocking(False)
    ret = None

    if delay is None: ready = select.select([sock], [], []) # Blocking
    else: ready = select.select([sock], [], [], delay/1000) # Non-blocking

    if ready[0]:
        ret = sock.recvfrom(1024)

    sock.setblocking(True)
    return ret

# Check whether the message is authentic
def auth(data, passwd):
    # data should contain "hmac" and "message"
    global history
    try:
        m = json.loads(data["message"])
        h = hmac.new((passwd + str(m["time"])).encode(),
                 data["message"].encode()).hexdigest()
        return data["hmac"].lower() == h.lower()
    except: return False

# Send ACK message to addr
def ack(addr, action, response):
    global sock
    global server_password

    # Check if action in history
    try:
        index = [x[0] for x in history].index(action)
        # Data is in history, resend ACK
        response = history[index][1]
    except:
        # Not found
        while (len(history) > HISTORY_SIZE): history.popleft()
        history.append((action, response))

    t = round(time.time() * 1000)
    msg = json.dumps({"action": action, "time": t,
                      "result": response[0], "message": response[1]})
    print("ACK", msg)
    h = hmac.new((server_password + str(t)).encode(), msg.encode()).hexdigest()
    sock.sendto(json.dumps({"hmac": h, "message": msg}).encode(), addr)

# Parses the message
def parse_data(data):
    global ROOT_DIR
    # Parse Command
    out = (False, None)
    try:
        if data["command"] == 'list':
            # List files in ROOT_DIR/data["directory"]
            path = os.path.join(ROOT_DIR, data["directory"])
            path = os.path.abspath(os.path.realpath((path)))
            if path[:len(ROOT_DIR)] != ROOT_DIR:
                # Outside of root
                out = (False, "Directory out of bounds")
            elif not os.path.isdir(path):
                # Not a directory
                out = (False, "%s is not a directory" % data["directory"])
            else:
                entries = os.listdir(path)
                if no_hidden:
                    entries = list(filter(lambda x: x[0] != '.', entries))
                dirs = list(filter(lambda x:
                    os.path.isdir(os.path.join(path, x)), entries))
                files = list(filter(lambda x:
                    os.path.isfile(os.path.join(path, x)), entries))
                if len(filetypes) != 0:
                    files = list(filter(lambda x:
                        x.split('.')[-1] in filetypes, entries))
                out = (True, {"directories": dirs, "files": files})
        elif data["command"] == 'play':
            path = os.path.join(ROOT_DIR, data["path"])
            path = os.path.abspath(os.path.realpath((path)))
            if path[:len(ROOT_DIR)] != ROOT_DIR:
                # Outside of root
                out = (False, "Path out of bounds")
            elif not os.path.isfile(path):
                # Not a file
                out = (False, "%s is not a file" % data["path"])
            else:
                # Start mpv
                play(path)
        elif data["command"] == 'get':
            # get property and send it back
            out = get_property(data["property"])
            out = (out != None, out)
        elif data["command"] == 'set':
            # set a single property
            out = set_property(data["property"], data["value"])
            if out == False:
                print("Error setting property: %s = %s" %
                        (data["property"], data["value"]))
            out = (out, None)
        elif data["command"] == 'show':
            # show a property
            if 'pre' in data and 'post' in data:
                out = show_property(data["property"],
                                    pre=data["pre"], post=data["post"])
            elif 'pre' in data:
                out = show_property(data["property"], pre=data["pre"])
            elif 'post' in data:
                out = show_property(data["property"], post=data["post"])
            else:
                out = show_property(data["property"])
            out = (out != None, out)
        else:
            if data["command"] not in COMMAND_WHITELIST:
                print("Command not in whitelist: %s" % data)
                out = (False, "Command not allowed")
            else:
                out = send_command(data["command"], data["args"])
                out = (out != None, out)
    except: pass
    return out

# State 0 of the FSM (normal mode)
def state_0(data):
    global repeat_done
    if data["command"] == 'repeat':
        repeat_done = False
        threading.Thread(target=repeat, args=[data], daemon=True).start()
        return (state_1, (True, "Repeating"))
    return (state_0, parse_data(data))

# State 1 of the FSM (repeat mode)
def state_1(data):
    global repeat_done
    if data["command"] == 'stop':
        # Kill thread
        repeat_done = True
        return (state_0, (True, "Stopping"))
    return (state_1, (False, "Waiting for stop"))

# Repeat a command until getting the 'stop' command
def repeat(data):
    args = data["args"]
    delay = 100
    if 'delay' in data: delay = data["delay"]
    while not repeat_done:
        send_command(args[0], args[1:])
        time.sleep(delay/1000)

def main():
    global sock
    global server_password
    global ROOT_DIR
    global no_hidden
    global filetypes

    parser = argparse.ArgumentParser(
            description=help_string,
            usage="%(prog)s [OPTIONS] password",
            formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument("-p", "--port", metavar="PORT_NUMBER", type=int,
                        default=28899,
                        help="Port number on which to listen")
    parser.add_argument("-s", "--mpv-socket", metavar="SOCK", type=str,
                        default="/tmp/mpvsocket",
                        help="Unix socket that mpv is listening on")
    parser.add_argument("-r", "--root", metavar="ROOT_DIR", type=str,
                        default=os.environ['HOME'],
                        help="Root directory for file browsing")
    parser.add_argument("-d", "--no-hidden", action='store_true',
                        help="Do not send hidden filenames")
    parser.add_argument("-f", "--filetypes", metavar="FILETYPES", type=str,
                        default="",
                        help=''.join(("Only send filetypes ",
                              "in the comma separated list FILETYPES. ",
                              "Blank means no filter.")))
    parser.add_argument("password", help="The password for this server")
    args = parser.parse_args()

    server_port = args.port
    mpv_socket = args.mpv_socket
    root = os.path.abspath(os.path.realpath((args.root)))
    if os.path.isdir(root):
        ROOT_DIR = root
    else:
        ROOT_DIR = os.environ['HOME']
        print("%s is not a directory or does not exist." % root)
        print("Using default root directory: %s" % ROOT_DIR)
    no_hidden = args.no_hidden
    filetypes = [x for x in args.filetypes.split(',') if len(x) > 0]
    server_password = args.password

    # Setup signal handler
    signal.signal(signal.SIGINT, cleanup)

    # Start listening on port
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('0.0.0.0', server_port))

    state = state_0

    while True:
        data, addr = sock.recvfrom(1024)
        try:
            data = json.loads(data.decode())
            # print("Connected from %s:%s" % (addr[0], addr[1]))

            # Authenticate
            if auth(data, server_password) == False: continue
            data = json.loads(data["message"])
            print(data)

            ret = None
            if data["time"] not in [x[0] for x in history]:
                state, ret = state(data)
            ack(addr, data["time"], ret)
        except:
            print("Error parsing command: %s" % data)

if __name__ == "__main__": main()
