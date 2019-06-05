import hmac
import unittest
from hashlib import md5
from utils import Messenger
from mpv_remote_app.media_server import MediaServer

class TestMediaServer(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # create messenger
        cls.hermes = Messenger('127.0.0.1', 12345, 'test')
        # start server
        cls.server_password = 'test'
        cls.server_root = 'tests/root'
        cls.server = MediaServer(12345, cls.server_password, cls.server_root)
        cls.server.run(daemon=True)
    @classmethod
    def tearDownClass(cls):
        cls.server.close()

    def test_init(self):
        self.assertEqual(self.server.port, 12345)
        self.assertEqual(self.server.password, self.server_password)
        self.assertTrue(self.server.root.endswith(self.server_root))
        self.assertIsNotNone(self.server.pid)
    def test_health(self):
        resp = self.hermes.send_raw("health")
        self.assertIsNotNone(resp)
        message = self.hermes.json_decode(self.hermes.json_decode(resp)['message'])
        self.assertTrue(message['result'])
    def test_hmac(self):
        resp = self.hermes.send_raw("health")
        self.assertIsNotNone(resp)
        data = self.hermes.json_decode(resp)
        message = self.hermes.json_decode(data['message'])

        sent_hmac = data['hmac']
        sent_time = message['time']
        key = (self.server_password + str(sent_time)).encode()
        expected_hmac = hmac.new(key, data['message'].encode(), md5).hexdigest()
        self.assertEqual(sent_hmac, expected_hmac)

if __name__ == '__main__':
    unittest.main()
