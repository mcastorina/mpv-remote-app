#!/usr/bin/python3

import sys
import json
import time
import hmac
import signal
import socket
import subprocess
from collections import deque

# Unix socket used to communicate with mpv
# mpv needs to be started with the flag --input-ipc-server SOCKET
SOCKET = "/tmp/mpvsocket"

# UDP socket to communicate with app
sock = None
# Password for the server
password = None
# History of commands
history = deque()
# Max size of history
HISTORY_SIZE = 32

# Whitelist for commands (besides get / set / show property)
COMMAND_WHITELIST = ["seek", "show_text", "cycle pause"]


# Usage message
def print_help():
    print(
        "Usage: %s PORT PASSWORD\r\n\r\n"
        "Listens on PORT for UDP commands to execute shell scripts\r\n\r\n"
        "PASSWORD sets the password of this server and must be used\r\n"
        "by the client to alter the state of mpv\r\n" %
        (sys.argv[0])
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

# Send message to mpv (on sock) via socat
def socat(command, sock=SOCKET):
    ret, out = call(["echo", "'%s'" % command, '|', "socat", "-", sock])
    return out

# Get the property from the mpv listening on sock
def get_property(property, sock=SOCKET):
    try:
        cmd = json.dumps({"command": ["get_property", property]})
        out = json.loads(socat(cmd, sock))
        if out["error"] != "success":
            return None
        return out["data"]
    except: return None

# Set the property on the mpv listening on sock
def set_property(property, value, sock=SOCKET):
    try:
        cmd = json.dumps({"command": ["set_property", property, value]})
        out = json.loads(socat(cmd, sock))
        if out["error"] != "success":
            return False
        return True
    except: return False

# Show the property (on OSD) on the mpv listening on sock
def show_property(property, pre=None, post="", sock=SOCKET):
    try:
        if pre is None:
            pre = property.title() + ": "
        arg = "\"%s${%s}%s\"" % (pre, property, post)
        return send_command("show_text", [arg], sock)
    except: return False

# Sends command to mpv listening on sock
def send_command(command, args, sock=SOCKET):
    try:
        args = [str(arg) for arg in args]
        cmd = "%s %s" % (command, ' '.join(args))
        return socat(cmd)
    except: return False

def auth(data, passwd):
    # data should contain "hmac" and "message"
    global history
    try:
        m = json.loads(data["message"])
        h = hmac.new((passwd + str(m["time"])).encode(),
                 data["message"].encode()).hexdigest()
        return data["hmac"].lower() == h.lower()
    except: return False

def ack(addr, response):
    global sock
    global password
    t = round(time.time() * 1000)
    msg = json.dumps({"action": "ACK", "result": response, "time": t})
    h = hmac.new((password + str(t)).encode(), msg.encode()).hexdigest()
    sock.sendto(json.dumps({"hmac": h, "message": msg}).encode(), addr)

def parse_data(data):
    # Parse Command
    out = None
    if data["command"] == 'get':
        # get property and send it back
        out = get_property(data["property"])
    elif data["command"] == 'set':
        # set a single property
        out = set_property(data["property"], data["value"])
        if out == False:
            print("Error setting property: %s = %s" %
                    (data["property"], data["value"]))
    elif data["command"] == 'show':
        # show a property
        out = show_property(data["property"], data["pre"], data["post"])
    else:
        if data["command"] not in COMMAND_WHITELIST:
            print("Command not in whitelist: %s" % data)
            out = False
        else:
            out = send_command(data["command"], data["args"])
    return out

def main():
    global sock
    global password

    server_port = 0;
    # Must be called with two arguments: PORT PASSWORD
    if (len(sys.argv) == 3):
        server_port = int(sys.argv[1])
        password = sys.argv[2]
    else:
        print_help()
        sys.exit(1)

    # Setup signal handler
    signal.signal(signal.SIGINT, cleanup)

    # Start listening on port
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('0.0.0.0', server_port))

    while True:
        data, addr = sock.recvfrom(1024)
        try:
            data = json.loads(data.decode())
            # print("Connected from %s:%s" % (addr[0], addr[1]))

            # Authenticate
            if auth(data, password) == False: continue
            data = json.loads(data["message"])
            print(data)

            # Check if data["time"] in history
            try:
                index = [x[0] for x in history].index(data["time"])
                # Data is in history
                # Resend ACK message
                ack(addr, history[index][1])
            except:
                # Not found
                ret = parse_data(data)
                while (len(history) > HISTORY_SIZE): history.popleft()
                history.append((data["time"], ret))
                # Send ACK
                ack(addr, ret)
        except:
            print("Error parsing command: %s" % data)

if __name__ == "__main__": main()
