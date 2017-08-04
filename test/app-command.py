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
    return json.loads(sock.recv(1024).decode())

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

# UDP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

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
