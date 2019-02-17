from collections import OrderedDict
from hashlib import md5
import subprocess
import threading
import logging
import socket
import json
import time

class MediaController:
    def __init__(self):
        pass
    def play(self, path):
        raise Exception('Not Implemented')
    def pause(self, state):
        raise Exception('Not Implemented')
    def unpause(self):
        return self.pause(False)
    def stop(self):
        raise Exception('Not Implemented')
    def seek(self, seconds):
        raise Exception('Not Implemented')
    def set_volume(self, volume):
        raise Exception('Not Implemented')
    def set_subtitles(self, track):
        raise Exception('Not Implemented')
    def set_audio(self, track):
        raise Exception('Not Implemented')
    def fullscreen(self, state):
        raise Exception('Not Implemented')
    def mute(self, state):
        raise Exception('Not Implemented')
    def unmute(self):
        return self.mute(False)
    def get_playback(self):
        raise Exception('Not Implemented')
    def get_pause(self):
        raise Exception('Not Implemented')
    def get_time(self):
        raise Exception('Not Implemented')
    def get_pos(self):
        raise Exception('Not Implemented')
    def get_volume(self):
        raise Exception('Not Implemented')
    def get_subtitles(self):
        raise Exception('Not Implemented')
    def get_audio(self):
        raise Exception('Not Implemented')
    def get_fullscreen(self):
        raise Exception('Not Implemented')
    def get_mute(self):
        raise Exception('Not Implemented')
    def get_subtitle_tracks(self):
        raise Exception('Not Implemented')
    def get_audio_tracks(self):
        raise Exception('Not Implemented')


class SocketMediaController(MediaController):
    def __init__(self, sock_addr):
        super().__init__()
        self.sock_addr = sock_addr
        self.sock = None
    def connect(self):
        self.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.sock.connect(self.sock_addr)
    def disconnect(self):
        self.sock.close()
        self.sock = None
    def connected(self):
        return self.sock is not None
    def send(self, message):
        if not self.connected(): self.connect()
        return self.sock.sendall(message.encode())
    def recv(self, timeout=None):
        if not self.connected(): self.connect()
        self.sock.settimeout(timeout)
        msg = ''
        try:
            while True:
                ret = self.sock.recv(4096).decode()
                if not ret:
                    return msg
                msg += ret
        except socket.timeout as e:
            return msg
        except:
            return None

class MpvController(SocketMediaController):
    GET_TRACKS_CACHE_SIZE = 16
    def __init__(self, unix_socket='/tmp/mpvsocket'):
        super().__init__(unix_socket)
        self._get_tracks_cache = OrderedDict()
        self.lock = threading.Lock()

    def play(self, path):
        ret = self.send_command('loadfile', [path])
        if ret:
            # prefetch audio and subtitle tracks
            threading.Thread(target=self.prefetch_tracks).start()
        return ret
    def pause(self, state=True):
        return self.set_property('pause', state)
    def stop(self):
        return self.send_command('stop')
    def seek(self, seconds):
        return self.send_command('seek %d' % seconds, raw=True)
    def set_volume(self, volume):
        return self.set_property('volume', volume)
    def set_subtitles(self, track):
        return self.set_property('sub', track)
    def set_audio(self, track):
        return self.set_property('audio', track)
    def fullscreen(self, state):
        return self.set_property('fullscreen', state)
    def mute(self, state=True):
        return self.set_property('mute', state)
    def get_playback(self):
        return self.get_property('filename')
    def get_pause(self):
        return self.get_property('pause')
    def get_time(self):
        return self.get_property('time-pos')
    def get_pos(self):
        return self.get_property('percent-pos')
    def get_volume(self):
        return self.get_property('volume')
    def get_subtitles(self):
        return self.get_property('sub')
    def get_audio(self):
        return self.get_property('audio')
    def get_fullscreen(self):
        return self.get_property('fullscreen')
    def get_mute(self):
        return self.get_property('mute')
    def get_subtitle_tracks(self):
        self.lock.acquire()
        ret = self._get_tracks('sub', ['id', 'lang'])
        self.lock.release()
        return ret
    def get_audio_tracks(self):
        self.lock.acquire()
        ret = self._get_tracks('audio', ['id', 'lang'])
        self.lock.release()
        return ret
    def prefetch_tracks(self):
        # retry due to file not loaded yet
        # TODO: change to wait for event
        self.lock.acquire()
        logging.debug('===================== PREFETCH START')
        for i in range(5):
            logging.debug("============= PREFETCH ATTEMPT %d", i)
            try:
                self._get_tracks('sub', ['id', 'lang'])
                self._get_tracks('audio', ['id', 'lang'])
                break
            except:
                time.sleep(0.2)
        logging.debug('===================== PREFETCH STOP')
        self.lock.release()

    # Get the property
    def get_property(self, property):
        try:
            cmd = json.dumps({'command': ['get_property', property]})
            out = json.loads(self._socat(cmd))
            if out['error'] != 'success':
                return None
            return out['data']
        except Exception as e:
            logging.debug("get_property exception: %s", str(e))
            return None

    # Set the property
    def set_property(self, property, value):
        try:
            cmd = json.dumps({'command': ['set_property', property, value]})
            out = json.loads(self._socat(cmd))
            if out['error'] != 'success':
                return False
            return True
        except Exception as e:
            logging.debug("set_property exception: %s", str(e))
            return False

    # Show the property (on OSD)
    def show_property(self, property, pre=None, post='', duration=1000):
        try:
            if pre is None:
                pre = property.title() + ': '
            if post is None:
                post = ''
            arg = '%s${%s}%s' % (pre, property, post)
            return self._socat('show_text "%s" %d' % (arg, duration))
        except Exception as e:
            logging.debug("show_property exception: %s", str(e))
            return None

    # Send command
    def send_command(self, command, args=[], raw=False):
        try:
            if raw:
                cmd = command
            else:
                cmd = json.dumps({"command": [command] + args})
            logging.debug("send_command: %s", cmd)
            return json.loads(self._socat(cmd))['error'] == 'success'
        except Exception as e:
            logging.debug("send_command exception: %s", str(e))
            return False

    def _socat(self, command, filter='error'):
        logging.debug("Mpv request: \"%s\"", command)
        self.send(command + '\n')
        ret = self.recv(0.05)
        logging.debug("Mpv response: \"%s\"", ret)
        if filter is not None:
            ret = '\n'.join([y for y in ret.split('\n') if filter in y]).strip()
            logging.debug("Filtered response: \"%s\"", ret)
        return ret

    def _get_tracks(self, track_type, info, fmt=None):
        path = self.get_property("path")
        hash = md5(''.join(map(str, [path, track_type, info, fmt])).encode()).hexdigest()
        if hash in self._get_tracks_cache:
            logging.debug("Hash \"%s\" found in _get_tracks_cache", hash)
            return self._get_tracks_cache.get(hash)

        tracks = []
        if not isinstance(info, list):
            info = [info]
        if fmt is None:
            start = '{}:' if len(info) > 1 else '{}'
            fmt = start + ' {}' * (len(info) - 1)

        n = int(self.get_property("track-list/count"))

        for i in range(n):
            if self.get_property("track-list/%d/type" % i) == track_type:
                data = [self.get_property("track-list/%d/%s" % (i, inf)) for inf in info]
                tracks += [fmt.format(*data)]

        logging.debug("Setting hash \"%s\" in _get_tracks_cache", hash)
        self._get_tracks_cache[hash] = tracks
        while len(self._get_tracks_cache) > MpvController.GET_TRACKS_CACHE_SIZE:
            # remove in FIFO order
            self._get_tracks_cache.popitem(last=False)
        return tracks
