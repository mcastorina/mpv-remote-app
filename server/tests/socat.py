#!/usr/bin/python3
# Attempt at reading and writing to the Unix socket directly

import socket
import json
import sys

SOCKET = "/tmp/mpvsocket"

# Send message to mpv (on sock_addr)
def socat(command):
    # Connect to socket
    mpv_sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    mpv_sock.connect(SOCKET)
    mpv_sock.settimeout(0.1)

    # Send / receive command
    mpv_sock.send((command + '\n').encode())
    ret = mpv_sock.recv(1024).decode()
    print(ret)

    # Close socket
    mpv_sock.close()
    return ret

def get_property(property):
    cmd = json.dumps({"command": ["get_property", property]})
    out = json.loads(socat(cmd))
    if out["error"] != "success":
        return None
    return out["data"]

def set_property(property, value):
    cmd = json.dumps({"command": ["set_property", property, value]})
    out = json.loads(socat(cmd))
    if out["error"] != "success":
        return False
    return True

def show_property(property, pre=None, post=""):
    if pre is None:
        pre = property.title() + ": "
    cmd = "show_text \"%s${%s}%s\"" % (pre, property, post)
    return socat(cmd)

def send_command(command, args):
    args = [str(arg) for arg in args]
    cmd = "%s %s" % (command, ' '.join(args))
    return socat(cmd)

while True:
    try:
        cmd = input(": ").split()
        try:
            if cmd[0] == "set":
                assert(len(cmd) >= 2)
                print(set_property(cmd[1], cmd[2]))
            elif cmd[0] == "get":
                assert(len(cmd) >= 1)
                print(get_property(cmd[1]))
            elif cmd[0] == "send":
                assert(len(cmd) >= 2)
                print(send_command(cmd[1], cmd[2:]))
            elif cmd[0] == "show":
                assert(len(cmd) >= 2)
                if len(cmd) == 2:
                    print(show_property(cmd[1]))
                elif len(cmd) == 3:
                    print(show_property(cmd[1], cmd[2]))
                else:
                    print(show_property(cmd[1], cmd[2], cmd[3]))
        except:
            print("Error")
    except (EOFError, KeyboardInterrupt):
        print("Exiting")
        sys.exit(0)
