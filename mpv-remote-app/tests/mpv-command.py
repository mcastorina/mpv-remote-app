#!/usr/bin/python3
# Program that continuously reads input to send to the mpv socket
# A list of properties can be found in properties.txt
# The first argument should be:
#   set  - set_property
#   get  - get_property
#   send - send_command
#   show - show_property
# Note: For boolean properties use "yes" and "no"

import subprocess
import json
import sys

SOCKET = "/tmp/mpvsocket"

def call(args):
    child = subprocess.Popen(' '.join(args), shell=True,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = child.communicate()
    ret = child.poll()
    return (ret, stdout.decode().strip())

def socat(command):
    ret, out = call(["echo", "'%s'" % command, '|', "socat", "-", SOCKET])
    return out

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
        sock.close()
        sys.exit(0)

