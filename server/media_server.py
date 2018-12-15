import os
import hmac
import json
import time
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

        # attributes that change per connection
        self.client = None
        self.action_id = None
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
                    self._ack(True, None)
                    continue
                # expect {message: "message", hmac: "HMAC"}
                data = json.loads(data)
                # authenticate
                if self._auth(data) == False:
                    logging.info("Authentication failed for command: \"%s\"", str(data))
                    continue
                data = json.loads(data["message"])
                # set action_id as the time of the request
                self.action_id = data["time"]
                logging.info("Received command: \"%s\"", data)
                return data
            except Exception as e:
                logging.error("Error parsing command: \"%s\"", data)
                logging.error(str(e))
    # sends response back to requester
    def _ack(self, success, response):
        # check if action in history

        # generate message
        t = round(time.time() * 1000)
        msg = json.dumps({"action": self.action_id, "time": t,
                          "result": success, "message": response})
        logging.debug("Sending ACK: \"%s\"", msg)

        h = hmac.new((self.password + str(t)).encode(), msg.encode()).hexdigest()
        self.sock.sendto(json.dumps({"hmac": h, "message": msg}).encode(), self.client)

        # clear client and action_id
        self.client = None
        self.action_id = None
    # authenticates data we received
    def _auth(self, data):
        # expect data = {message: "message", hmac: "HMAC"}
        try:
            m = json.loads(data["message"])
            h = hmac.new((self.password + str(m["time"])).encode(),
                     data["message"].encode()).hexdigest()
            return data["hmac"].lower() == h.lower()
        except: return False
