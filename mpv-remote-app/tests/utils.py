#!/usr/bin/python3

import hmac
import json
import time
import socket
from hashlib import md5

class Messenger:
    def __init__(self, addr, port, password):
        self.addr = (addr, port)
        self.password = password
        self.sock = None
        # UDP socket
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def send_command(self, command, args={}):
        args['command'] = command
        ret = self.send_message(args)
        return Messenger.json_decode(ret['message'])

    # Send dictionary data with timestamp t
    def send_message(self, data):
        # Add timestamp
        t = round(time.time() * 1000)
        data["time"] = t
        # Convert to string
        msg = json.dumps(data)
        # HMAC
        h = hmac.new((self.password + str(t)).encode(), msg.encode(), md5).hexdigest()
        raw_resp = self.send_raw(json.dumps({"hmac": h, "message": msg}))
        return Messenger.json_decode(raw_resp)

    def send_raw(self, raw, response=True):
        self.sock.sendto(raw.encode(), self.addr)
        if response:
            return self.sock.recv(4096).decode()
        return None

    @staticmethod
    def json_decode(data):
        return json.loads(data)
