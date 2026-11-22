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

ROOT = Path(__file__).resolve().parents[1]


def run(*command, **kwargs):
    print('+', ' '.join(map(str, command)), flush=True)
    return subprocess.run(list(map(str, command)), check=True, **kwargs)


def main():
    from PIL import Image, ImageChops
    sdk = Path(os.environ['ANDROID_HOME'])
    tools = sdk / 'build-tools/35.0.0'
    android_jar = sdk / 'platforms/android-35/android.jar'
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
              text.setBackgroundColor(android.graphics.Color.BLACK);
              text.setTextColor(android.graphics.Color.WHITE);
              text.setText("Launcher ready"); setContentView(text);
            }
          }''')
        apk, aligned = t / 'base.apk', t / 'aligned.apk'
        run(tools / 'aapt2', 'link', '-o', apk, '--manifest', manifest, '-I', android_jar,
            '--min-sdk-version', '28', '--target-sdk-version', '35', '-0', 'mp4', '-A', t / 'assets')
        run('javac', '--release', '8', '-classpath', android_jar, '-d', t / 'classes',
            ROOT / 'packaging/android/java/dev/horizon/android/IntroActivity.java', stub)
        classes = list((t / 'classes').rglob('*.class'))
        run(tools / 'd8', '--min-api', '28', '--lib', android_jar, '--output', t / 'dex', *classes)
        with zipfile.ZipFile(apk, 'a') as z:
            z.write(t / 'dex/classes.dex', 'classes.dex')
        run(tools / 'zipalign', '-f', '4', apk, aligned)
        keystore = t / 'test.jks'
        run('keytool', '-genkeypair', '-keystore', keystore, '-storepass', 'android', '-keypass', 'android',
            '-alias', 'test', '-keyalg', 'RSA', '-validity', '1', '-dname', 'CN=Emulator Test')
        run(tools / 'apksigner', 'sign', '--ks', keystore, '--ks-pass', 'pass:android', aligned)
        run('adb', 'install', '-r', aligned)
        run('adb', 'logcat', '-c')
        run('adb', 'shell', 'am', 'start', '-W', '-n', 'vp.net.voltplus.introtest/dev.horizon.android.IntroActivity')
        # Frames at several points avoid racing the first black frame or codec preparation.
        seen_video = False
        rendering_started = False
        bright_frames = []
        for i in range(12):
            time.sleep(0.38)
            logs = subprocess.check_output(['adb', 'logcat', '-d', '-s', 'VoltIntro:I'], text=True)
            rendering_started |= 'Intro video rendering started' in logs
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
                if rendering_started and ratio > 0.01:
                    seen_video = True
                    bright_frames.append(center.resize((160, 90)))
        # The default system splash icon is static. Require multiple bright,
        # changing decoded frames so that it cannot make this test pass.
        animated = any(
            sum(1 for pixel in ImageChops.difference(a, b).convert('L').getdata() if pixel > 20)
            > 160 * 90 * 0.01
            for i, a in enumerate(bright_frames) for b in bright_frames[i + 1:]
        )
        if not rendering_started or not seen_video or not animated:
            run('adb', 'logcat', '-d', '-s', 'VoltIntro:*', 'MediaPlayer:*')
            raise SystemExit('No changing decoded intro-logo frames were visible on the emulator')
        time.sleep(5)
        activity = subprocess.check_output(['adb', 'shell', 'dumpsys', 'activity', 'activities'], text=True)
        if 'dev.horizon.android.HorizonActivity' not in activity:
            raise SystemExit('Video did not hand off to the launcher')
        print('Intro was visible and handed off to the launcher.')


if __name__ == '__main__':
    main()
