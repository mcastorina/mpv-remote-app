#!/usr/bin/python3
# A program that continuously reads input to simulate commands sent from
# the Android app
# A list of properties can be found in properties.txt
# The first argument should be:
#   set  - set_property
#   get  - get_property
#   send - send_command
#   show - show_property
# Note: For boolean properties use "yes" and "no"

import sys
import hmac
import json
import time
import socket
import traceback

SERVER_PASSWORD = "hello"
SERVER_ADDR = ('127.0.0.1', 28899)
sock = None

# Send dictionary data with timestamp t
def send_message(data):
    global sock
    # Add timestamp
    t = round(time.time() * 1000)
    data["time"] = t

    # Convert to string
    msg = json.dumps(data)

    # HMAC
    h = hmac.new((SERVER_PASSWORD + str(t)).encode(), msg.encode()).hexdigest()
    sock.sendto(json.dumps({"hmac": h, "message": msg}).encode(), SERVER_ADDR)
    return json.loads(sock.recv(4096).decode())

def get_property(property):
    return send_message({
        "command": "get",
        "property": property
    })

def set_property(property, value):
    return send_message({
        "command": "set",
        "property": property,
        "value": value
    })

def send_command(command, args):
    return send_message({
        "command": command,
        "args": args
    })

def show_property(property, pre=None, post=""):
    return send_message({
        "command": "show",
        "property": property,
        "pre": pre,
        "post": post
    })

def ls(directory):
    return send_message({
        "command": "list",
        "directory": directory
    })

def play(path):
    return send_message({
        "command": "play",
        "path": path
    })

def print_json(j):
    j["message"] = json.loads(j["message"])
    print(json.dumps(j, sort_keys=True, indent=4, separators=(', ', ': ')))

# UDP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

while True:
    try:
        cmd = input(": ").split()
        try:
            if cmd[0] == "set":
                assert(len(cmd) >= 2)
                print_json(set_property(cmd[1], cmd[2]))
            elif cmd[0] == "get":
                assert(len(cmd) >= 1)
                print_json(get_property(cmd[1]))
            elif cmd[0] == "send":
                assert(len(cmd) >= 2)
                print_json(send_command(cmd[1], cmd[2:]))
            elif cmd[0] == "show":
                assert(len(cmd) >= 2)
                if len(cmd) == 2:
                    print_json(show_property(cmd[1]))
                elif len(cmd) == 3:
                    print_json(show_property(cmd[1], cmd[2]))
                else:
                    print_json(show_property(cmd[1], cmd[2], cmd[3]))
            elif cmd[0] == "ls":
                if len(cmd) == 1: cmd += ["."]
                for i in range(1, len(cmd)):
                    print_json(ls(cmd[i]))
            elif cmd[0] == "play":
                assert(len(cmd) == 2)
                print_json(play(cmd[1]))
        except:
            traceback.print_exc()
    except (EOFError, KeyboardInterrupt):
        print("Exiting")
        sock.close()
        sys.exit(0)
