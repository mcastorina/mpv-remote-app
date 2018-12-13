import os
import socket
from collections import OrderedDict

class MediaServer:
    def __init__(self, port, password, root, no_hidden=True, filetypes=None):
        self.port = port            # port to listen on
        self.password = password    # server secret
        self.root = root            # top level directory of server
        self.no_hidden = no_hidden  # send / play hidden files
        self.filetypes = filetypes  # array of acceptable filetypes

        # create socket
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(('0.0.0.0', self.port))
        # setup history and state
        self.state = 'NORMAL'       # state can be NORMAL or REPEAT
        self.history_size = 32      # max number of commands to remember
        self.history = OrderedDict()
    # runs the server
    def run(self, daemon=False):
        pass
    # stops the server
    def stop(self):
        pass
    # returns whether the server is running or not
    def is_running(self):
        pass
    # receives a command
    def _recv(self):
        pass
    # sends response back to requester
    def _ack(self):
        pass
    # authenticates data we received
    def _auth(self, data):
        pass
