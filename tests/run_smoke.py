import pathlib,re,subprocess,sys,time
root=pathlib.Path(__file__).resolve().parents[1]
server=subprocess.Popen(['java','-Dzkm.web.home='+str(root/'build/web-smoke'/('home-'+str(time.time_ns()))),'-jar',str(root/'build/libs/zkm27.jar'),'--web','--no-browser'],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
try:
    line=server.stdout.readline()
    match=re.search(r'(http://127.0.0.1:\d+)/#token=(\w+)',line)
    if not match:raise RuntimeError(line)
    subprocess.run([sys.executable,str(root/'tests/smoke_web.py'),match[1],match[2]],cwd=root,check=True)
finally:
    server.terminate()
    try:server.wait(timeout=10)
    except subprocess.TimeoutExpired:server.kill()
