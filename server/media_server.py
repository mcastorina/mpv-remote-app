import os
import json
import socket
import logging
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
    # receives a command and authenticates the message
    def _recv(self):
        # loop until we receive a valid command
        while True:
            data, self.client = self.sock.recvfrom(1024)
            try:
                data = data.decode()
                logging.debug("Received: \"%s\"", data)
                if data == "health":
                    logging.debug("alive")
                    self._ack(True)
                    continue
                # expect {message: "message", hmac: "HMAC"}
                data = json.loads(data)
                # authenticate
                if self._auth(data) == False:
                    logging.info("Authentication failed for command: \"%s\"", str(data))
                    continue
                data = json.loads(data["message"])
                logging.info("Received command: \"%s\"", data)
                return data
            except Exception as e:
                logging.error("Error parsing command: \"%s\"", data)
                logging.error(str(e))
    # sends response back to requester
    def _ack(self, success):
        self.sock.sendto(str(success).encode(), self.client)
    # authenticates data we received
    def _auth(self, data):
        return True
