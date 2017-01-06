#!/usr/bin/python3

import sys
import json
import signal
import socket
import subprocess

# Unix socket used to communicate with mpv
# mpv needs to be started with the flag --input-ipc-server SOCKET
SOCKET = "/tmp/mpvsocket"

# UDP socket to communicate with app
sock = None

# Whitelist for commands (besides get / set / show property)
COMMAND_WHITELIST = ["seek", "show_text", "cycle pause"]


# Usage message
def print_help():
    print(
        "Usage: %s PORT RECEIVE_IP\r\n\r\n"
        "Listens on PORT for UDP commands to execute shell scripts\r\n\r\n"
        "RECEIVE_IP is the IP address of the computer this program will\r\n"
        "accept commands from. It is being used as a form of preknown\r\n"
        "authentication.\r\n" %
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
    cmd = json.dumps({"command": ["get_property", property]})
    out = json.loads(socat(cmd, sock))
    if out["error"] != "success":
        return None
    return out["data"]

# Set the property on the mpv listening on sock
def set_property(property, value, sock=SOCKET):
    cmd = json.dumps({"command": ["set_property", property, value]})
    out = json.loads(socat(cmd, sock))
    if out["error"] != "success":
        return False
    return True

# Show the property (on OSD) on the mpv listening on sock
def show_property(property, pre=None, post="", sock=SOCKET):
    if pre is None:
        pre = property.title() + ": "
    arg = "\"%s${%s}%s\"" % (pre, property, post)
    return send_command("show_text", [arg], sock)

# Sends command to mpv listening on sock
def send_command(command, args, sock=SOCKET):
    args = [str(arg) for arg in args]
    cmd = "%s %s" % (command, ' '.join(args))
    return socat(cmd)

def main():
    global sock

    server_port = 0;
    # Must be called with two arguments: PORT RECEIVE_IP
    if (len(sys.argv) == 3):
        server_port = int(sys.argv[1])
        receive_ip = sys.argv[2]
    else:
        print_help()
        sys.exit(1)

    # Setup signal handler
    signal.signal(signal.SIGINT, cleanup)

    # Start listening on port
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(('0.0.0.0', server_port))

    # TODO: authentication
    #   Current authentication is just checking IP. A better approach
    #   would be to use public key encryption, agree on a symmetric key,
    #   and use that for all communication

    while True:
        data, addr = sock.recvfrom(1024)
        try:
            data = json.loads(data.decode())
            # print("Connected from %s:%s" % (addr[0], addr[1]))
            # Ignore any commands not from receive_ip
            if addr[0] != receive_ip: continue

            if data["command"] == 'get':
                # get property and send it back
                out = get_property(data["property"])
                sock.sendto(out.encode(), addr)
            elif data["command"] == 'set':
                # set a single property
                out = set_property(data["property"], data["value"])
                if out == False:
                    print("Error setting property: %s = %s" %
                            (data["property"], data["value"]))
            elif data["command"] == 'show':
                # show a property
                show_property(data["property"], data["pre"], data["post"])
            else:
                if data["command"] not in COMMAND_WHITELIST:
                    print("Command not in whitelist: %s" % data)
                    continue
                send_command(data["command"], data["args"])
        except:
            print("Error parsing command: %s" % data)

if __name__ == "__main__": main()
