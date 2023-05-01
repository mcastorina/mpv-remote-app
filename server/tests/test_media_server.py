import hmac
import unittest
from unittest import mock
from hashlib import md5
from utils import Messenger
from mpv_remote_app.media_server import MediaServer
from mpv_remote_app.media_controllers import MpvController

class TestMediaServer(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # create messenger
        cls.hermes = Messenger('127.0.0.1', 12345, 'test')
        # start server
        cls.server_password = 'test'
        cls.server_root = 'tests/root'
        cls.server = MediaServer(12345, '127.0.0.1', cls.server_password, cls.server_root)
        cls.server.run(daemon=True)
    @classmethod
    def tearDownClass(cls):
        cls.server.close()

    def setUp(self):
        # mock the MpvController
        self.server.controller = mock.create_autospec(MpvController)

    def test_init(self):
        self.assertEqual(self.server.port, 12345)
        self.assertEqual(self.server.password, self.server_password)
        self.assertTrue(self.server.root.endswith(self.server_root))
        self.assertIsNotNone(self.server.pid)
    def test_health(self):
        resp = self.hermes.send_raw("health")
        self.assertIsNotNone(resp)
        message = Messenger.json_decode(Messenger.json_decode(resp)['message'])
        self.assertTrue(message['result'])
    def test_hmac(self):
        resp = self.hermes.send_raw("health")
        self.assertIsNotNone(resp)
        data = Messenger.json_decode(resp)
        message = Messenger.json_decode(data['message'])

        sent_hmac = data['hmac']
        sent_time = message['time']
        key = (self.server_password + str(sent_time)).encode()
        expected_hmac = hmac.new(key, data['message'].encode(), md5).hexdigest()
        self.assertEqual(sent_hmac, expected_hmac)
    def test_serve(self):
        ret = self.hermes.send_command('')
        self.assertFalse(ret['result'])
        self.assertEqual(ret['message'], 'Not Implemented')

    def test_play(self):
        self.server._serve({'command': 'play', 'path': './file1.mp4'}, ack=False)
        self.server.controller.play.assert_called()
    def test_play_oob(self):
        self.server._serve({'command': 'play', 'path': '../../test_media_server.py'}, ack=False)
        self.server.controller.play.assert_not_called()
    def test_play_non_file(self):
        self.server._serve({'command': 'play', 'path': './dir1'}, ack=False)
        self.server.controller.play.assert_not_called()
    def test_play_unicode(self):
        self.server._serve({'command': 'play', 'path': './τεστ/αβξ.mkv'}, ack=False)
        self.server.controller.play.assert_called()
    def test_pause(self):
        self.server._serve({'command': 'pause', 'state': True}, ack=False)
        self.server.controller.pause.assert_called_with(True)
        self.server._serve({'command': 'pause', 'state': False}, ack=False)
        self.server.controller.pause.assert_called_with(False)
    def test_stop(self):
        self.server._serve({'command': 'stop'}, ack=False)
        self.server.controller.stop.assert_called()
    def test_seek(self):
        self.server._serve({'command': 'seek', 'seconds': 5}, ack=False)
        self.server.controller.seek.assert_called_with(5)
    def test_volume(self):
        self.server._serve({'command': 'set_volume', 'volume': 0}, ack=False)
        self.server.controller.set_volume.assert_called_with(0)
    def test_subtitles(self):
        self.server._serve({'command': 'set_subtitles', 'track': 0}, ack=False)
        self.server.controller.set_subtitles.assert_called_with(0)
    def test_audio(self):
        self.server._serve({'command': 'set_audio', 'track': 0}, ack=False)
        self.server.controller.set_audio.assert_called_with(0)

    def test_list(self):
        ret, msg = self.server._serve({'command': 'list', 'directory': '.'}, ack=False)
        self.assertTrue(ret)
        self.assertEqual(sorted(msg['directories']), ['dir1', 'τεστ'])
        self.assertEqual(sorted(msg['files']), ['file1.mp4', 'file2.mkv', 'file3.txt'])
    def test_list_oob(self):
        ret, msg = self.server._serve({'command': 'list', 'directory': '..'}, ack=False)
        self.assertFalse(ret)
        self.assertEqual(msg, 'Directory out of bounds')
    def test_list_filter(self):
        self.server.filetypes = ['mp4', 'mkv']
        ret, msg = self.server._serve({'command': 'list', 'directory': '.'}, ack=False)
        self.assertTrue(ret)
        self.assertEqual(sorted(msg['directories']), ['dir1', 'τεστ'])
        self.assertEqual(sorted(msg['files']), ['file1.mp4', 'file2.mkv'])
    def test_list_unicode(self):
        ret, msg = self.server._serve({'command': 'list', 'directory': 'τεστ'}, ack=False)
        self.assertTrue(ret)
        self.assertEqual(msg['directories'], [])
        self.assertEqual(msg['files'], ['αβξ.mkv'])


if __name__ == '__main__':
    unittest.main()
