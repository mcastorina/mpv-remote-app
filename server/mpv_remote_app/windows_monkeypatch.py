# no AF_UNIX sockets for windows
# looking forward to https://bugs.python.org/issue33408
# using a monkeypatch for now
import win32pipe
from .media_controllers import MpvController, MediaController


class PipeMediaController(MediaController):
    def __init__(self, pipename):
        super().__init__()
        self.pipename = pipename
        self.last_answer = ''

    def send(self, message):
        self.last_answer = win32pipe.CallNamedPipe(
            self.pipename, 
            message.encode(), 
            1024, 
            100,
        )
        return len(self.last_answer) != 0

    def recv(self, timeout=None):
        return self.last_answer.decode()


MpvController.__bases__ = (PipeMediaController, )
# `show_property` commands have no response from mpv (https://mpv.io/manual/master/#socat-example)
# CallNamedPipe hangs indefinitely awaiting for a response despite a specified timeout
# thus removing `show_property` method
MpvController.show_property = lambda self, property, pre=None, post='', duration=1000: None
