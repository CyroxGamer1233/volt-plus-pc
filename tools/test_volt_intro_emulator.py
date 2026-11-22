#!/usr/bin/env python3
"""Build a tiny APK with the real intro Activity, and check its pixels on an emulator.

The launcher is a stub so an x86 emulator can test the arm64 release's video UI
without loading the game's architecture-specific native library.
"""
import os
from pathlib import Path
import subprocess
import tempfile
import time
import zipfile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ['ANDROID_HOME'])
TOOLS = SDK / 'build-tools/35.0.0'
ANDROID_JAR = SDK / 'platforms/android-35/android.jar'


def run(*command, **kwargs):
    print('+', ' '.join(map(str, command)), flush=True)
    return subprocess.run(list(map(str, command)), check=True, **kwargs)


def main():
    with tempfile.TemporaryDirectory() as temporary:
        t = Path(temporary)
        (t / 'assets').mkdir()
        (t / 'classes').mkdir()
        (t / 'dex').mkdir()
        (t / 'assets/intro.mp4').write_bytes((ROOT / 'packaging/android/media/intro.mp4').read_bytes())
        manifest = t / 'AndroidManifest.xml'
        manifest.write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="vp.net.voltplus.introtest">
          <application android:theme="@android:style/Theme.Material.NoActionBar" android:label="Intro test">
            <activity android:name="dev.horizon.android.HorizonActivity" android:exported="false" android:screenOrientation="landscape" />
            <activity android:name="dev.horizon.android.IntroActivity" android:exported="true" android:screenOrientation="landscape">
              <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
            </activity>
          </application></manifest>''')
        stub = t / 'HorizonActivity.java'
        stub.write_text('''package dev.horizon.android;
          public final class HorizonActivity extends android.app.Activity {
            @Override public void onCreate(android.os.Bundle state) {
              super.onCreate(state);
              android.widget.TextView text = new android.widget.TextView(this);
              text.setText("Launcher ready"); setContentView(text);
            }
          }''')
        apk, aligned = t / 'base.apk', t / 'aligned.apk'
        run(TOOLS / 'aapt2', 'link', '-o', apk, '--manifest', manifest, '-I', ANDROID_JAR,
            '--min-sdk-version', '28', '--target-sdk-version', '35', '-0', 'mp4', '-A', t / 'assets')
        run('javac', '--release', '8', '-classpath', ANDROID_JAR, '-d', t / 'classes',
            ROOT / 'packaging/android/java/dev/horizon/android/IntroActivity.java', stub)
        classes = list((t / 'classes').rglob('*.class'))
        run(TOOLS / 'd8', '--min-api', '28', '--lib', ANDROID_JAR, '--output', t / 'dex', *classes)
        with zipfile.ZipFile(apk, 'a') as z:
            z.write(t / 'dex/classes.dex', 'classes.dex')
        run(TOOLS / 'zipalign', '-f', '4', apk, aligned)
        keystore = t / 'test.jks'
        run('keytool', '-genkeypair', '-keystore', keystore, '-storepass', 'android', '-keypass', 'android',
            '-alias', 'test', '-keyalg', 'RSA', '-validity', '1', '-dname', 'CN=Emulator Test')
        run(TOOLS / 'apksigner', 'sign', '--ks', keystore, '--ks-pass', 'pass:android', aligned)
        run('adb', 'install', '-r', aligned)
        run('adb', 'shell', 'am', 'start', '-W', '-n', 'vp.net.voltplus.introtest/dev.horizon.android.IntroActivity')
        # Frames at several points avoid racing the first black frame or codec preparation.
        seen_video = False
        for i in range(12):
            time.sleep(0.38)
            result = subprocess.run(['adb', 'exec-out', 'screencap', '-p'], capture_output=True, check=True)
            image_path = t / f'frame-{i}.png'
            image_path.write_bytes(result.stdout)
            with Image.open(image_path) as image:
                image = image.convert('RGB')
                w, h = image.size
                center = image.crop((int(w * .22), int(h * .15), int(w * .78), int(h * .80)))
                bright = sum(1 for r, g, b in center.getdata() if r > 150 and g > 150 and b > 150)
                ratio = bright / (center.width * center.height)
                print(f'Frame {i}: {w}x{h}, bright center fraction {ratio:.4f}', flush=True)
                seen_video |= ratio > 0.01
        if not seen_video:
            run('adb', 'logcat', '-d', '-s', 'VoltIntro:*', 'MediaPlayer:*')
            raise SystemExit('No bright intro-logo pixels were visible on the emulator')
        time.sleep(5)
        activity = subprocess.check_output(['adb', 'shell', 'dumpsys', 'activity', 'activities'], text=True)
        if 'dev.horizon.android.HorizonActivity' not in activity:
            raise SystemExit('Video did not hand off to the launcher')
        print('Intro was visible and handed off to the launcher.')


if __name__ == '__main__':
    main()
