import subprocess
import socket
import json

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
    def fullscreen(self, state):
        raise Exception('Not Implemented')
    def mute(self, state):
        raise Exception('Not Implemented')
    def unmute(self):
        return self.mute(False)

class SocketMediaController(MediaController):
    def __init__(self, sock_addr):
        super().__init__()
        self.sock_addr = sock_addr
    def connect(self):
        self.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.sock.connect(self.sock_addr)
    def disconnect(self):
        self.sock.close()
    def send(self, message):
        return self.sock.sendall(message.encode())
    def recv(self, timeout=None):
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
    def __init__(self, unix_socket='/tmp/mpvsocket'):
        super().__init__(unix_socket)
        self.connect()

    def play(self, path):
        return self.send_command('loadfile', [path])

    def pause(self, state=True):
        return self.set_property('pause', state)

    def stop(self):
        return self.send_command('stop')

    def seek(self, seconds):
        return self.send_command('seek', [seconds])

    def set_volume(self, volume):
        return self.set_property('volume', volume)

    def set_subtitles(self, track):
        return self.set_property('sub', track)

    def fullscreen(self, state):
        return self.set_property('fullscreen', state)

    def mute(self, state=True):
        return self.set_property('mute', state)

    # Get the property
    def get_property(self, property):
        try:
            cmd = json.dumps({'command': ['get_property', property]})
            out = json.loads(self._socat(cmd).split('\n')[0])
            if out['error'] != 'success':
                return None
            return out['data']
        except: return None

    # Set the property
    def set_property(self, property, value):
        try:
            cmd = json.dumps({'command': ['set_property', property, value]})
            out = json.loads(self._socat(cmd).split('\n')[0])
            if out['error'] != 'success':
                return False
            return True
        except: return False

    # Show the property (on OSD)
    def show_property(self, property, pre=None, post=''):
        try:
            if pre is None:
                pre = property.title() + ': '
            arg = '"%s${%s}%s"' % (pre, property, post)
            return self.send_command('show_text', [arg])
        except: return None

    # Send command
    def send_command(self, command, args=[]):
        try:
            args = [str(arg) for arg in args]
            cmd = '%s %s' % (command, ' '.join(args))
            return self._socat(cmd)
        except: return False

    def _socat(self, command):
        # TODO: use super class to send / recv
        self.send(command + '\n')
        return self.recv(0.05)

# Run command and return (exit code, stdout)
def _call(args):
    child = subprocess.Popen(' '.join(args), shell=True,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = child.communicate()
    ret = child.poll()
    return (ret, stdout.decode().strip())
