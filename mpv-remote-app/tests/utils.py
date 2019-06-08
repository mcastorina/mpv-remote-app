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
        return self.json_decode(raw_resp)

    def send_raw(self, raw, response=True):
        self.sock.sendto(raw.encode(), self.addr)
        if response:
            return self.sock.recv(4096).decode()
        return None

    def json_decode(self, data):
        return json.loads(data)
