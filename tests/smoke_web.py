"""Exercise the packaged app. All generated files live under build/web-smoke."""
import json, os, pathlib, re, subprocess, time, urllib.request, sys, zipfile, shutil
ROOT=pathlib.Path(__file__).resolve().parents[1]
base=sys.argv[1]; token=sys.argv[2]
def api(path,data=None):
    req=urllib.request.Request(base+'/api/v1'+path,data=None if data is None else json.dumps(data).encode(),headers={'X-ZKM-Token':token,'Content-Type':'application/json'})
    try:
        with urllib.request.urlopen(req,timeout=180) as r:return json.load(r)
    except urllib.error.HTTPError as e:raise RuntimeError(e.read().decode()) from e

def run(data,ok=True):
    j=api('/jobs',data)
    deadline=time.time()+180
    while j['status'] in ('queued','running'):
        if time.time()>deadline:raise RuntimeError('Job timeout: '+j['id'])
        time.sleep(.25);j=api('/jobs/'+j['id'])
    print(data['kind'],j['status'],j.get('error','').splitlines()[0] if j.get('error') else '',flush=True)
    if ok and j['status']!='succeeded':
        log=pathlib.Path(j['directory'])/'console.log'
        if log.exists():print(log.read_text()[-8000:])
        raise AssertionError(j)
    return j

root=ROOT/'build/web-smoke'/str(time.time_ns());root.mkdir(parents=True)
fixture=root/'fixture';fixture.mkdir()
(fixture/'Hello.java').write_text('public class Hello { public static int count=7; public static void main(String[] a){ System.out.println(message()); } public static String message(){ return "Hello workbench " + count; }}')
subprocess.run(['javac','--release','8','-d',str(fixture),str(fixture/'Hello.java')],check=True)
subprocess.run(['jar','cfe',str(root/'input.jar'),'Hello','-C',str(fixture),'Hello.class'],check=True)
with zipfile.ZipFile(root/'input.jar','a') as archive:archive.writestr('assets/info.txt','resource preserved')
p=api('/projects/create',{'directory':str(root/'project'),'name':'Smoke project'})
p['build']['inputs']=[str(root/'input.jar')]
p=api('/project',p)
run({'kind':'validate','script':'print "hello";','sourcePath':'scripts/test.zkm'})
run({'kind':'validate','script':'obfuscate badOption=true;'},False)
run({'kind':'open'})
assert api('/engine',{'operation':'tree'})['nodes'][0]['name']=='Hello'
report=run({'kind':'trimReport'})
assert 'trim' in report['result']['text'].lower(), report

classes=api('/engine',{'operation':'classes'})
assert classes['total']==1,classes
cid=classes['classes'][0]['id']
cl=api('/engine',{'operation':'class','classId':cid})
assert any(m['name']=='main' for m in cl['methods'])
run({'kind':'edit','classId':cid,'member':'count','descriptor':'I','name':'counter','flags':9,'editKind':'field'})
cl=api('/engine',{'operation':'class','classId':cid})
assert any(f['name']=='counter' for f in cl['fields'])
literal=next(c for c in cl['constants'] if c['value']=='Hello workbench ')
run({'kind':'edit','classId':cid,'editKind':'constant','index':literal['index'],'value':'Edited workbench '})
p=api('/project')
api('/projects/open',{'directory':p['directory']})
cl=api('/engine',{'operation':'classes'})
restored=api('/engine',{'operation':'class','classId':cl['classes'][0]['id']})
assert any(f['name']=='counter' for f in restored['fields'])
original=run({'kind':'inspect'})
assert api('/comparison?jobId='+original['id'])['before'][0]['name']=='Hello'
j=run({'kind':'preview'})
pairs=api('/comparison?jobId='+j['id'])['classes'];assert pairs and pairs[0]['after'],pairs
for engine in ('vf','cfr'):
    for side in ('before','after'):
        target=pairs[0][side]
        d=run({'kind':'decompile','jobId':j['id'],'side':side,'classId':target['id'],'engine':engine})
        assert len(d['result']['source'])>30
run({'kind':'bytecode','jobId':j['id'],'side':'before','classId':pairs[0]['before']['id']})
api('/comparison/manual',{'jobId':j['id'],'pairId':pairs[0]['id'],'afterId':pairs[0]['after']['id']})
assert api('/comparison?jobId='+j['id'])['manual'][pairs[0]['id']]==pairs[0]['after']['id']
j=run({'kind':'build'})
output=root/'project/output/input.jar'
assert output.exists(),list((root/'project').rglob('*'))
with zipfile.ZipFile(output) as archive:assert archive.read('assets/info.txt')==b'resource preserved'
result=subprocess.run(['java','-jar',str(output)],text=True,capture_output=True,check=True)
assert result.stdout.strip()=='Edited workbench 7',result
runtime=api('/runtime/config?jobId='+j['id'])
outputs=[]
for side in ('before','after'):
    execution=run({'kind':'programRun','sourceJob':j['id'],'side':side,'classpath':runtime[side], 'mainClass':'', 'workingDirectory':runtime['workingDirectory'],'timeout':10})
    assert execution['result']['exitCode']==0,execution
    outputs.append(execution['result']['stdout'])
assert outputs[0]==outputs[1] and outputs[0].strip()=='Edited workbench 7',outputs
run({'kind':'proguard','text':'-keep public class Hello { public static void main(java.lang.String[]); }'})

# Preserve execute semantics during preview, including relative working directory.
(fixture/'ExecMarker.java').write_text('import java.nio.file.*; public class ExecMarker { public static void main(String[] a) throws Exception { Files.writeString(Path.of("execute-marker.txt"),"ran"); }}')
subprocess.run(['javac','-d',str(fixture),str(fixture/'ExecMarker.java')],check=True)
run({'kind':'preview','script':'execute "java -cp '+fixture.as_posix()+' ExecMarker";'})
assert (root/'project/execute-marker.txt').read_text()=='ran'
# Existing jars + ChangeLog should also pair and decompile.
run({'kind':'compareImport','before':str(root/'input.jar'),'after':str(output),'changeLogs':[str(root/'project/ChangeLog.txt')]})
# Cancellation must stop a blocking script and leave the HTTP server usable.
(fixture/'Sleeper.java').write_text('public class Sleeper { public static void main(String[] a) throws Exception { Thread.sleep(60000); }}')
subprocess.run(['javac','-d',str(fixture),str(fixture/'Sleeper.java')],check=True)
program=api('/jobs',{'kind':'programRun','classpath':[str(fixture)],'mainClass':'Sleeper','workingDirectory':str(fixture),'timeout':60})
time.sleep(1)
api('/jobs/'+program['id']+'/cancel',{})
assert api('/jobs/'+program['id'])['status']=='cancelled'
pending=api('/jobs',{'kind':'script','script':'execute "java -cp '+fixture.as_posix()+' Sleeper";'})
time.sleep(1)
api('/jobs/'+pending['id']+'/cancel',{})
assert api('/jobs/'+pending['id'])['status']=='cancelled'
assert api('/state')['project']['name']=='Smoke project'

# Unsaved scripts retain their include directory, including spaces and Chinese names.
source=root/'project'/'中文 scripts';source.mkdir()
(source/'包含.zkm').write_text('print "include ok";',encoding='utf-8')
run({'kind':'script','sourcePath':str(source/'未保存.zkm'),'script':'<%include "包含.zkm"%>\nprint "after include";'})
assert not (source/'未保存.zkm').exists()
failed=run({'kind':'script','script':'open "missing-file.jar";'},False)
assert failed['status']=='failed'
# Editing conflicts are rejected without touching the active repository.
try:api('/jobs',{'kind':'edit','projectRevision':-1})
except RuntimeError as error:assert 'CONFLICT' in str(error)
else:raise AssertionError('Expected revision conflict')
# Scanner keeps archive origins, nested locations and multi-release identities separate.
with zipfile.ZipFile(root/'multi.jar','w') as z:
    z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nMulti-Release: true\n\n')
    z.write(fixture/'Hello.class','Hello.class')
    z.write(fixture/'Hello.class','META-INF/versions/17/Hello.class')
    z.write(root/'input.jar','nested/dependency.jar')
imported=run({'kind':'compareImport','before':str(root/'multi.jar'),'after':str(root/'multi.jar'),'changeLogs':[]})
comparison=api('/comparison?jobId='+imported['id'])
assert len(comparison['before'])==3
assert len({c['id'] for c in comparison['before']})==3
# Class working copies, Trim and sequential obfuscation use real snapshots.
p=api('/project');p['checkpointInputs']=[];p['build']['inputs']=[str(ROOT/'demo/SnakeGame.jar')]
p['build']['trim']=True;p=api('/project',p)
snake=run({'kind':'preview'})
assert api('/comparison?jobId='+snake['id'])['before']
# Original CLI still accepts scripts and preserves their semantics.
cli=root/'cli.zkm';cli.write_text('print "CLI compatibility";')
result=subprocess.run(['java','-jar',str(ROOT/'build/libs/zkm27.jar'),str(cli)],cwd=root,text=True,capture_output=True,timeout=60)
assert result.returncode==0 and 'CLI compatibility' in result.stdout,result

# Compare symbols, bytecode instructions and behavior; the legacy engine reorders constant pools even between two CLI invocations.
p=api('/project');p['checkpointInputs']=[];p['build']['inputs']=[str(root/'input.jar')];p['build']['trim']=False
p['build']['obfuscation']={'randomize':'false','obfuscateFlow':'none','encryptStringLiterals':'none','lineNumbers':'keep'}
p['build']['output']=str(root/'deterministic-web');p=api('/project',p)
web_run=run({'kind':'build'})
cli_project=json.loads(json.dumps(p));cli_project['build']['output']=str(root/'deterministic-cli')
cli_script=api('/build/script',{'project':cli_project})['script'];cli.write_text(cli_script)
subprocess.run(['java','-jar',str(ROOT/'build/libs/zkm27.jar'),str(cli)],cwd=root,capture_output=True,check=True,timeout=60)
def instructions(archive):
    with zipfile.ZipFile(archive) as z:names=sorted(n[:-6].replace('/','.') for n in z.namelist() if n.endswith('.class'))
    listing=subprocess.check_output(['javap','-classpath',str(archive),'-p','-c','-s','-l',*names],text=True)
    return re.sub(r'[ \t]+//', ' //', re.sub(r'#\d+', '#constant', listing))
assert instructions(root/'deterministic-web/input.jar')==instructions(root/'deterministic-cli/input.jar')
for flavor in ('web','cli'):
    execution=subprocess.check_output(['java','-jar',str(root/('deterministic-'+flavor)/'input.jar')],text=True)
    assert execution.strip()=='Hello workbench 7'
# Multiple obfuscations retain the original class pairing.
script=api('/build/script',{'project':p,'mode':'transform'})['script']+'\nobfuscate randomize=false obfuscateFlow=none encryptStringLiterals=none;'
multiple=run({'kind':'preview','script':script})
assert all(pair['after'] for pair in api('/comparison?jobId='+multiple['id'])['classes'] if pair['before'])
# Move the project, reopen, and decompile a historical snapshot at its new location.
moved=root/'relocated project';shutil.copytree(root/'project',moved)
api('/projects/open',{'directory':str(moved)})
pairs=api('/comparison?jobId='+web_run['id'])['classes']
assert str(moved) in pairs[0]['before']['file']
run({'kind':'decompile','jobId':web_run['id'],'side':'before','classId':pairs[0]['before']['id'],'engine':'cfr'})
print('SMOKE PASSED',root,flush=True)



