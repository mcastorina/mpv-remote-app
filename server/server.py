#!/usr/bin/python3

import os
import sys
import json
import time
import hmac
import signal
import socket
import select
import psutil
import argparse
import threading
import subprocess
from collections import deque
from media_controllers import MpvController

# global mpv controller
mpv = None

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
COMMAND_WHITELIST = ["seek", "show_text", "cycle pause", "stop", "loadfile"]


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
                res = mpv.play('"%s"' % path)
                if res == "":
                    out = (True, None)
                else:
                    out = (False, "play returned %s" % res)
        elif data["command"] == 'get':
            # get property and send it back
            out = mpv.get_property(data["property"])
            out = (out != None, out)
        elif data["command"] == 'set':
            # set a single property
            out = mpv.set_property(data["property"], data["value"])
            if out == False:
                print("Error setting property: %s = %s" %
                        (data["property"], data["value"]))
            out = (out, None)
        elif data["command"] == 'show':
            # show a property
            if 'pre' in data and 'post' in data:
                out = mpv.show_property(data["property"],
                                    pre=data["pre"], post=data["post"])
            elif 'pre' in data:
                out = mpv.show_property(data["property"], pre=data["pre"])
            elif 'post' in data:
                out = mpv.show_property(data["property"], post=data["post"])
            else:
                out = mpv.show_property(data["property"])
            out = (out != None, out)
        elif data["command"] == 'health':
            out = (True, None)
        else:
            if data["command"] not in COMMAND_WHITELIST:
                print("Command not in whitelist: %s" % data)
                out = (False, "Command not allowed")
            else:
                out = mpv.send_command(data["command"], data["args"])
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
    auto_speedup = True
    counter = 0
    if 'delay' in data:
        delay = data["delay"]
        auto_speedup = False
    while not repeat_done:
        mpv.send_command(args[0], args[1:])
        time.sleep(delay/1000)
        if auto_speedup:
            counter += 1
            # speedup every 3 second
            if (counter * delay) >= 3000:
                # hack to avoid sending so many commands for seek
                if args[0] == "seek":
                    args[1] = str(int(args[1]) * 2)
                else:
                    delay = max(delay/2, 1)
                counter = 0

def main():
    global sock
    global mpv
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
                        help="Only send filetypes "
                              "in the comma separated list FILETYPES. "
                              "Blank means no filter.")
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

    # Check if mpv is running and spawn if not
    found_mpv = False
    for pid in psutil.pids():
        try:
            p = psutil.Process(pid)
            name, cmdline = p.cmdline()
            if p.name() == "mpv" and "--input-ipc-server" in cmdline:
                mpv_socket = cmdline[cmdline.index("--input-ipc-server") + 1]
                found_mpv = True
                break
        except: pass
    if not found_mpv:
        print("No running mpv instance found. Starting mpv...")
        subprocess.Popen(["mpv", "--no-terminal",
            "--input-ipc-server", mpv_socket, "--idle"])
        time.sleep(0.2)
    mpv = MpvController(mpv_socket)

    # Start listening on port
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('0.0.0.0', server_port))

    state = state_0
    while True:
        data, addr = sock.recvfrom(1024)
        try:
            data = data.decode()
            if data == "health":
                print(data)
                ack(addr, None, (True, None))
                continue
            data = json.loads(data)
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
