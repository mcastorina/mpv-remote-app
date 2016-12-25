#!/usr/bin/python3

import socket
import sys
import signal
from subprocess import call

sock = None

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
        # print("Connected from %s:%s" % (addr[0], addr[1]))
        # Ignore any commands not from receive_ip
        if addr[0] != receive_ip: continue

        try:
            command, id = data.decode().split()
            if command == 'key':
                # key letter
                call(['xdotool', 'key', id])
            elif command == 'mouse_move':
                # mouse_move x,y
                call(['xdotool', 'mousemove_relative', '--'] + id.split(','))
            elif command == 'mouse_click':
                # mouse_click number
                call(['xdotool', 'click', id])
        except:
            print("Error parsing command: %s" % data.decode())

if __name__ == "__main__": main()
