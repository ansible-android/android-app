"""Qualify an immutable standalone APK in a disposable Android emulator."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

OUT = Path('release-qualification')
PACKAGE = 'su.ansible.messenger.web'
CERTIFICATE = 'a08d7dc323ddf71ef3201944397e0d3cce7d40847263e11f328b68bbe19229ab'
PREVIOUS_SHA = '19e874f3879d34f475626b602085ff2e4e744d6c95eae13ee13907f5fa5790e1'


def command(args, **kwargs):
    p = subprocess.run(args, capture_output=True, timeout=kwargs.pop('timeout', 120), **kwargs)
    if p.returncode:
        # Process output can contain account or device diagnostics.
        raise RuntimeError('Android qualification command failed: ' + args[0])
    return p.stdout


def gh(path):
    return json.loads(command(['gh', 'api', path]))


def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for data in iter(lambda: f.read(1024 * 1024), b''):
            h.update(data)
    return h.hexdigest()


def main():
    source = os.environ['APK_SOURCE_SHA']
    run = os.environ['APK_BUILD_RUN']
    repo = os.environ['GITHUB_REPOSITORY']
    assert re.fullmatch('[a-f0-9]{40}', source) and run.isdigit()
    if '--publish' in sys.argv:
        publish(repo, source, run)
        return
    OUT.mkdir(exist_ok=False)
    info = gh(f'repos/{repo}/actions/runs/{run}')
    assert info['status'] == 'completed' and info['conclusion'] == 'success'
    assert info['head_sha'] == source and info['head_repository']['full_name'] == repo
    assert info['path'] == '.github/workflows/build-debug.yml'
    artifacts = gh(f'repos/{repo}/actions/runs/{run}/artifacts')['artifacts']
    assert len(artifacts) == 1 and not artifacts[0]['expired']
    artifact = artifacts[0]
    assert 0 < artifact['size_in_bytes'] < 512 * 1024 * 1024
    archive = OUT/'build.zip'
    archive.write_bytes(command(['gh', 'api', f'repos/{repo}/actions/artifacts/{artifact["id"]}/zip'], timeout=600))
    assert 'sha256:' + sha(archive) == artifact['digest']
    apk = OUT/'ansible-0.4.3.apk'
    with zipfile.ZipFile(archive) as z:
        files = [x for x in z.infolist() if not x.is_dir()]
        assert len(files) == 1 and files[0].filename.endswith('.apk')
        assert files[0].file_size < 512 * 1024 * 1024
        apk.write_bytes(z.read(files[0]))
    archive.unlink()
    sdk = Path(os.environ['ANDROID_HOME'])
    tools = sdk/'build-tools/36.0.0'
    cert = command([str(tools/'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)]).decode()
    assert re.search(r'Signer #1 certificate SHA-256 digest: (\w+)', cert).group(1).lower() == CERTIFICATE
    badging = command([str(tools/'aapt'), 'dump', 'badging', str(apk)]).decode()
    assert f"package: name='{PACKAGE}' versionCode='30040039' versionName='0.4.3'" in badging
    with zipfile.ZipFile(apk) as z:
        names = set(z.namelist())
        for abi in ['arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64']:
            assert f'lib/{abi}/libtwidget.so' in names
            assert any(n.startswith(f'lib/{abi}/libansible.') for n in names)
    previous = OUT/'previous.apk'
    with urllib.request.urlopen(f'https://github.com/{repo}/releases/download/v0.3.1/ansible-0.3.1.apk', timeout=180) as response:
        previous.write_bytes(response.read())
    assert sha(previous) == PREVIOUS_SHA
    old_cert = command([str(tools/'apksigner'), 'verify', '--print-certs', str(previous)]).decode()
    assert re.search(r'Signer #1 certificate SHA-256 digest: (\w+)', old_cert).group(1).lower() == CERTIFICATE
    command(['avdmanager', 'create', 'avd', '--force', '--name', 'ansible043', '--package',
             'system-images;android-36;google_apis;x86_64', '--device', 'pixel_2'], input=b'no\n')
    emulator_log = (OUT/'emulator.log').open('wb')
    emulator = subprocess.Popen([str(sdk/'emulator/emulator'), '-avd', 'ansible043', '-no-window', '-no-audio',
                                 '-no-snapshot', '-no-boot-anim', '-gpu', 'swiftshader_indirect'],
                                stdout=emulator_log, stderr=subprocess.STDOUT)
    try:
        command(['adb', 'wait-for-device'], timeout=240)
        for _ in range(180):
            if command(['adb', 'shell', 'getprop', 'sys.boot_completed']).strip() == b'1':
                break
            time.sleep(2)
        else:
            raise RuntimeError('Disposable emulator did not boot')
        command(['adb', 'root'])
        command(['adb', 'wait-for-device'])
        # The launch/upgrade test needs no server account or production data.
        command(['adb', 'shell', 'svc', 'wifi', 'disable'])
        command(['adb', 'shell', 'svc', 'data', 'disable'])
        command(['adb', 'install', str(previous)])
        launch('previous')
        command(['adb', 'shell', 'am', 'force-stop', PACKAGE])
        base = f'/data/user/0/{PACKAGE}'
        prefs = base+'/shared_prefs/mainconfig.xml'
        data = command(['adb', 'shell', 'cat', prefs])
        tree = ET.fromstring(data)
        for child in list(tree):
            if child.get('name') == 'fons_size':
                tree.remove(child)
        ET.SubElement(tree, 'int', name='fons_size', value='19')
        local_prefs = OUT/'upgrade-mainconfig.xml'
        local_prefs.write_bytes(ET.tostring(tree, encoding='utf-8', xml_declaration=True))
        owner = command(['adb', 'shell', 'stat', '-c', '%u:%g', prefs]).decode().strip()
        assert re.fullmatch(r'\d+:\d+', owner)
        command(['adb', 'push', str(local_prefs), prefs])
        command(['adb', 'shell', 'chown', owner, prefs])
        marker = base+'/files/release043-upgrade-marker'
        command(['adb', 'shell', 'sh', '-c', f'"echo preserved-043 > {marker}"'])
        command(['adb', 'shell', 'chown', owner, marker])
        command(['adb', 'install', '-r', str(apk)])
        assert command(['adb', 'shell', 'cat', marker]).strip() == b'preserved-043'
        launch('upgraded')
        upgraded = ET.fromstring(command(['adb', 'shell', 'cat', prefs]))
        assert any(x.get('name') == 'fons_size' and x.get('value') == '19' for x in upgraded)
        (OUT/'upgraded.png').write_bytes(command(['adb', 'exec-out', 'screencap', '-p']))
        command(['adb', 'shell', 'am', 'force-stop', PACKAGE])
        command(['adb', 'uninstall', PACKAGE])
        command(['adb', 'install', str(apk)])
        launch('fresh')
        (OUT/'fresh.png').write_bytes(command(['adb', 'exec-out', 'screencap', '-p']))
        proof = dict(source=source, build_run=run, artifact_id=artifact['id'],
                     artifact_sha256=artifact['digest'], apk_sha256=sha(apk), certificate_sha256=CERTIFICATE,
                     version='0.4.3', version_code=30040039, base_version_code=3004003, four_native_abis=True,
                     fresh_launch=True, upgrade_launch=True, previous_version='0.3.1',
                     upgrade_file_retained=True, configured_font_size_retained=True,
                     online_authentication_tested=False, published=False)
        (OUT/'qualification.json').write_text(json.dumps(proof, indent=2)+'\n')
        previous.unlink()
        local_prefs.unlink()
        print(json.dumps(proof), flush=True)
    finally:
        subprocess.run(['adb', 'emu', 'kill'], capture_output=True, timeout=30)
        try:
            emulator.wait(timeout=30)
        except subprocess.TimeoutExpired:
            emulator.kill()
        emulator_log.close()


def launch(label):
    command(['adb', 'logcat', '-b', 'crash', '-c'])
    command(['adb', 'shell', 'am', 'start', '-W', '-n', PACKAGE+'/org.ansible.ui.LaunchActivity'])
    time.sleep(30)
    assert command(['adb', 'shell', 'pidof', PACKAGE]).strip(), label+' process did not survive'
    crashes = command(['adb', 'logcat', '-b', 'crash', '-d']).decode(errors='replace')
    assert PACKAGE not in crashes and 'org.ansible' not in crashes, label+' application crashed'
    activities = command(['adb', 'shell', 'dumpsys', 'activity', 'activities']).decode(errors='replace')
    assert PACKAGE in activities and 'LaunchActivity' in activities


def publish(repo, source, run):
    assert os.environ['APK_PUBLISH'] == 'true'
    proof = json.loads((OUT/'qualification.json').read_text())
    apk = OUT/'ansible-0.4.3.apk'
    assert proof['source'] == source and proof['build_run'] == run and sha(apk) == proof['apk_sha256']
    feeds = []
    for name in ['current2', 'current6']:
        with urllib.request.urlopen('https://ansible.su/ad/'+name, timeout=30) as response:
            feeds.append(json.load(response))
    assert feeds[0] == feeds[1]
    assert all(feeds[0][p]['stable']['released'] >= 3004003 for p in ['win64', 'mac', 'armac'])
    result = subprocess.run(['gh', 'api', f'repos/{repo}/releases/tags/v0.4.3'], capture_output=True)
    assert result.returncode and b'HTTP 404' in result.stderr, 'Release exists or absence is unproven'
    notes = OUT/'notes.md'
    notes.write_text('Ansible Android 0.4.3\n\nUpdated to upstream 13.2.2 and layer 233 while preserving Ansible design and features.\n\n'
                     f'Source: `{source}`. APK SHA-256: `{proof["apk_sha256"]}`.\n'
                     'Signature continuity, all four native architectures, fresh startup and upgrade data retention verified.\n')
    command(['gh', 'release', 'create', 'v0.4.3', '--repo', repo, '--target', source,
             '--title', 'Ansible Android 0.4.3', '--notes-file', str(notes), str(apk)])
    proof['published'] = True
    (OUT/'publication.json').write_text(json.dumps(proof, indent=2)+'\n')


if __name__ == '__main__':
    main()
