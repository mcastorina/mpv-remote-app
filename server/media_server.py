import os
import hmac
import json
import time
import signal
import socket
import logging
import threading
from media_controllers import *
from collections import OrderedDict
from os.path import abspath, realpath, join, isdir, isfile

class MediaServer:
    def __init__(self, port, password, root=os.getcwd(), no_hidden=True, filetypes=None, controller=None):
        self.port = port                # port to listen on
        self.password = password        # server secret
        self.root = abspath(realpath(root)) # top level directory of server
        self.no_hidden = no_hidden      # send / play hidden files
        self.filetypes = filetypes      # array of acceptable filetypes
        self.controller = controller    # MediaController

        # create socket
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.bind(('0.0.0.0', self.port))
        # setup history and state
        self.state = 'NORMAL'       # state can be NORMAL or REPEAT
        self.history_size = 4       # max number of commands to remember
        self.history = OrderedDict()
        # setup pid for daemon
        self.pid = 0

        # attributes that change per connection
        self.client = None
        self.action_id = None
    # runs the server
    def run(self, daemon=False):
        self.pid = 0
        if daemon:
            self.pid = os.fork()
        if self.pid == 0:
            while True:
                try:
                    logging.debug("heartbeat")
                    self._serve(self._recv())
                except KeyboardInterrupt:
                    self.stop()
                    break
        return True
    # stops the server
    def stop(self):
        logging.debug("shutting server down")
        if self.pid != 0:
            os.kill(self.pid, signal.SIGTERM)
            self.pid = 0
    # returns whether the server is running or not
    def is_running(self):
        return self.pid != 0
    # receives a new command and authenticates the message
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
                if self.action_id in self.history:
                    self._ack()
                    continue
                return data
            except Exception as e:
                logging.error("Error parsing command: \"%s\"", data)
                logging.error(str(e))
    # sends response back to requester
    def _ack(self, success=None, response=None):
        # check if action in history
        try:
            response = self.history[self.action_id]
            logging.debug("Found response in history for action_id \"%s\": \"%s\"",
                self.action_id, response)
        except:
            # generate message
            t = round(time.time() * 1000)
            msg = json.dumps({"action": self.action_id, "time": t,
                              "result": success, "message": response})
            logging.debug("Sending ACK: \"%s\"", msg)

            h = hmac.new((self.password + str(t)).encode(), msg.encode()).hexdigest()
            response = json.dumps({"hmac": h, "message": msg}).encode()
            if self.action_id is not None:
                # save in history
                self.history[self.action_id] = response

                # clear out old items
                while len(self.history) > self.history_size:
                    # pop items off in FIFO order
                    self.history.popitem(last=False)

        # send response
        self.sock.sendto(response, self.client)

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
    # take action to (already authenticated) command
    def _serve(self, cmd, ack=True):
        try:
            command = cmd["command"]

            # ret / msg will get changed by following cases
            ret, msg = False, None
            if command == "play":
                path = join(self.root, cmd["path"])
                if path[:len(self.root)] != self.root:
                    # outside of root
                    ret, msg = False, "Path out of bounds"
                elif not isfile(path):
                    # not a file
                    ret, msg  = False, "%s is not a file" % cmd["path"]
                else:
                    # play the file
                    ret = self.controller.play(abspath(realpath(path)))
                    if ret:
                        # save available audio and subtitle tracks
                        # TODO: sometimes this fails due to opening too soon
                        # temporary fix: retry in _get_tracks
                        tracks = {}
                        tracks["subtitle"] = self.controller.get_subtitle_tracks()
                        tracks["audio"] = self.controller.get_audio_tracks()
                        msg = tracks
            elif command == "pause":
                ret = self.controller.pause(cmd["state"])
            elif command == "stop":
                if self.state == 'REPEAT':
                    self.state = 'NORMAL'
                    ret, msg = True, "Stopping"
                else:
                    ret = self.controller.stop()
            elif command == "seek":
                ret = self.controller.seek(cmd["seconds"])
            elif command == "set_volume":
                ret = self.controller.set_volume(cmd["volume"])
            elif command == "set_subtitles":
                ret = self.controller.set_subtitles(cmd["track"])
            elif command == "set_audio":
                ret = self.controller.set_audio(cmd["track"])
            elif command == "fullscreen":
                ret = self.controller.fullscreen(cmd["state"])
            elif command == "mute":
                ret = self.controller.mute(cmd["state"])
            elif command == "repeat":
                self.state = 'REPEAT'
                threading.Thread(target=self._repeat, args=[cmd],
                        daemon=True).start()
                ret = True
            elif command == "list":
                ret, msg = self._list(cmd["directory"])
            elif command == "show":
                # display text on screen
                ret = self._show(cmd)
            else:
                ret, msg = False, "Not Implemented"

            # send back ack if required
            if ack: self._ack(ret, msg)
        except KeyError as e:
            self._ack(False, "Exception '%s'" % str(e))
        except Exception as e:
            logging.debug("%s", e)
            self._ack(False, "Not Implemented")
    def _list(self, opath):
        # list files in self.root/path
        path = abspath(realpath(join(self.root, opath)))
        if path[:len(self.root)] != self.root:
            # outside of root
            return (False, "Directory out of bounds")
        if not isdir(path):
            # not a directory
            return (False, "%s is not a directory" % opath)
        entries = os.listdir(path)
        if self.no_hidden:
            entries = list(filter(lambda x: x[0] != '.', entries))
        dirs = list(filter(lambda x: isdir(join(path, x)), entries))
        files = list(filter(lambda x: isfile(join(path, x)), entries))
        if self.filetypes and len(self.filetypes) > 0:
            files = list(filter(lambda x:
                x.split('.')[-1] in self.filetypes, entries))
        return (True, {"directories": dirs, "files": files})
    def _show(self, command):
        # only supported in MpvController
        supported = [MpvController]
        if any(map(lambda y: isinstance(self.controller, y), supported)):
            pre, post = None, ''
            if "pre" in command: pre = command["pre"]
            if "post" in command: post = command["post"]
            return self.controller.show_property(command["property"], pre, post)
    def _repeat(self, cmd):
        delay = 0.1
        speedup = True
        if "delay" in cmd:
            delay = cmd["delay"] / 1000
        if "speedup" in cmd:
            speedup = cmd["speedup"]
        args = cmd["args"]
        command, pairs = args[0], args[1:]

        if command != "seek":
            logging.info("Repeat command \"%s\" not supported", command)
            return

        cmd = {"command": command}
        for i in range(0, len(pairs), 2):
            cmd[pairs[i]] = pairs[i+1]

        counter = 0
        while self.state == 'REPEAT':
            logging.debug("REPEAT: %s", str(cmd))
            self._serve(cmd, ack=False)
            time.sleep(delay)
            if speedup:
                counter += 1
                # speedup every 3 second
                if (counter * delay) >= 2:
                    cmd["seconds"] = round(cmd["seconds"] * 2.5)
                    counter = 0
