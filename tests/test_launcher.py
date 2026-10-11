"""Packaged launcher tests; no external websites or browser installation required."""
import json, pathlib, re, socket, subprocess, tempfile, unittest, urllib.request
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAR=str(ROOT/'build/libs/zkm27.jar')
class LauncherTest(unittest.TestCase):
    def test_default_headless_fallback_and_shutdown(self):
        with tempfile.TemporaryDirectory(prefix='zkm-launcher-') as home:
            server=subprocess.Popen(['java','-Djava.awt.headless=true','-Dzkm.web.home='+home,'-jar',JAR],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
            try:
                line=server.stdout.readline();match=re.search(r'(http://127.0.0.1:\d+)/#token=(\w+)',line);self.assertIsNotNone(match,line)
                self.assertIn('--swing',server.stdout.readline());self.assertIn('Open the URL',server.stdout.readline())
                with urllib.request.urlopen(match[1]+'/') as response:self.assertIn(b'<html',response.read())
                request=urllib.request.Request(match[1]+'/api/v1/shutdown',data=b'{}',headers={'X-ZKM-Token':match[2]})
                with urllib.request.urlopen(request) as response:self.assertTrue(json.load(response)['stopping'])
                self.assertEqual(server.wait(timeout=10),0)
            finally:
                if server.poll() is None:server.kill();server.wait()
                server.stdout.close()
    def test_port_conflict_and_invalid_port(self):
        with socket.socket() as occupied:
            occupied.bind(('127.0.0.1',0));occupied.listen()
            result=subprocess.run(['java','-jar',JAR,'--web','--no-browser','--port',str(occupied.getsockname()[1])],capture_output=True,text=True,timeout=15)
            self.assertNotEqual(result.returncode,0);self.assertIn('Web UI startup failed',result.stderr)
        result=subprocess.run(['java','-jar',JAR,'--port','-1'],capture_output=True,text=True,timeout=15)
        self.assertNotEqual(result.returncode,0);self.assertIn('0..65535',result.stderr)
if __name__=='__main__':unittest.main()
