import unittest
from utils import Messenger
from mpv_remote_app.media_server import MediaServer

class TestMediaServer(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # create messenger
        cls.hermes = Messenger('127.0.0.1', 12345, 'test')
        # start server
        cls.server = MediaServer(12345, 'test', './tests/root')
        cls.server.run(daemon=True)
    @classmethod
    def tearDownClass(cls):
        cls.server.close()

    def test_init(self):
        self.assertEqual(self.server.port, 12345)
        self.assertEqual(self.server.password, 'test')
        self.assertTrue(self.server.root.endswith('/tests/root'))
        self.assertIsNotNone(self.server.pid)
    def test_health(self):
        resp = self.hermes.send_raw("health")
        self.assertIsNotNone(resp)
        message = self.hermes.json_decode(self.hermes.json_decode(resp)['message'])
        self.assertTrue(message['result'])

if __name__ == '__main__':
    unittest.main()
