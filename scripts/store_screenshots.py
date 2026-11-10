"""Captures the Google Play screenshots on an emulator, for every store language.

Usage (from the repository root, with the debug app installed on a running emulator):

    ./gradlew installDebug
    python scripts/store_screenshots.py [--serial emulator-5554] [--copy] [en-US pl-PL de-DE]

The script clears the app's data, fills in a ball set and some tubes, switches the status bar to
demo mode and the screen to 1080 x 2160 px, takes the screenshots and restores the device settings.
Images go to build/store-screenshots/<locale>/; with --copy they also replace
fastlane/metadata/android/<locale>/images/phoneScreenshots/.

Because it clears the app's data, it runs only on an emulator unless --allow-device is given.
"""
import argparse
import os
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from html import unescape

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PKG = "pl.stalmasinski.ballpassage"
RES = os.path.join(ROOT, "app", "src", "main", "res")
OUT = os.path.join(ROOT, "build", "store-screenshots")
FASTLANE = os.path.join(ROOT, "fastlane", "metadata", "android")
ADB = "adb"
SERIAL = None
SEP = "."

LOCALES = {
    "en-US": ("en-US", "values", "K-12", "case 2", 7),
    "pl-PL": ("pl-PL", "values-pl", "K-12", "walizka 2", 5),
    "de-DE": ("de-DE", "values-de", "K-12", "Koffer 2", 5),
}


def adb(*args, check=True):
    r = subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True)
    if check and r.returncode:
        raise RuntimeError(f"adb {args}: {r.stderr.decode(errors='replace')}")
    return r.stdout


def sh(cmd):
    return adb("shell", cmd).decode("utf-8", "replace")


def nap(s):
    time.sleep(s)


def strings(values_dir):
    out = {}
    for d in ("values", values_dir):
        for e in ET.parse(os.path.join(RES, d, "strings.xml")).getroot().findall("string"):
            out[e.get("name")] = "".join(e.itertext())
    return out


def dump():
    for _ in range(6):
        if "dumped" in sh("uiautomator dump /sdcard/ui.xml"):
            x = adb("exec-out", "cat /sdcard/ui.xml").decode("utf-8", "replace")
            nodes = []
            for n in re.findall(r"<node [^>]*>", x):
                b = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
                t = unescape(re.search(r' text="([^"]*)"', n).group(1))
                nodes.append({"text": t, "cls": re.search(r'class="([^"]*)"', n).group(1), "b": b})
            return nodes
        nap(1)
    raise RuntimeError("no ui dump")


def center(n):
    b = n["b"]
    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2


def find(text):
    return next((n for n in dump() if n["text"] == text), None)


def wait_text(text, timeout=10):
    end = time.time() + timeout
    while time.time() < end:
        n = find(text)
        if n:
            return n
        nap(0.5)
    raise RuntimeError(f"text not found: {text!r}; on screen: {[n['text'] for n in dump() if n['text']]}")


def tap_xy(x, y):
    sh(f"input tap {x} {y}")


def tap(text):
    tap_xy(*center(wait_text(text)))
    nap(0.8)


def fill(index, value):
    edits = [n for n in dump() if "EditText" in n["cls"]]
    b = edits[index]["b"]
    tap_xy((b[0] + b[2]) // 3, (b[1] + b[3]) // 2)
    nap(0.5)
    sh("input keyevent KEYCODE_MOVE_END")
    sh("input keyevent " + " ".join(["KEYCODE_DEL"] * 10))
    sh("input text " + value.replace(" ", "%s"))
    nap(0.4)


def ime_shown():
    return "mInputShown=true" in sh("dumpsys input_method")


def hide_ime():
    if ime_shown():
        sh("input keyevent KEYCODE_BACK")
        nap(1.2)


def swipe(y1, y2):
    sh(f"input swipe 540 {y1} 540 {y2} 500")
    nap(0.8)


def to_top():
    for _ in range(3):
        sh("input swipe 540 700 540 1800 150")
        nap(0.3)
    nap(0.5)


def shot(path):
    nap(0.8)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(adb("exec-out", "screencap -p"))
    print("  saved", os.path.relpath(path, OUT), flush=True)


def demo(command, **extras):
    args = " ".join(f"-e {k} {v}" for k, v in extras.items())
    sh(f"am broadcast -a com.android.systemui.demo -e command {command} {args}")


def setup():
    sh("wm size 1080x2160")
    sh("settings put secure show_ime_with_hard_keyboard 1")
    sh("settings put global sysui_demo_allowed 1")
    demo("enter")
    demo("clock", hhmm="1000")
    demo("battery", level="100", plugged="false")
    demo("network", wifi="show", level="4", fully="true")
    demo("network", mobile="show", datatype="lte", level="4", fully="true")
    demo("status", vpn="hide", location="hide", alarm="hide", sync="hide", bluetooth="hide", volume="hide", mute="hide", speakerphone="hide", cast="hide", hotspot="hide", tty="hide", eri="hide", managed_profile="hide", zen="hide")
    demo("notifications", visible="false")
    nap(1)


def teardown():
    sh("cmd uimode night no")
    demo("exit")
    sh("settings put global sysui_demo_allowed 0")
    sh("settings put secure show_ime_with_hard_keyboard 0")
    sh("wm size reset")
    sh(f"cmd locale set-app-locales {PKG} --locales ''")


def enter_tube(od, wall, radius, settle=3.0):
    # Empty the radius first: with an incomplete tube there is no result, so slow typing
    # cannot leave half-changed tubes in the history.
    edits = [n for n in dump() if "EditText" in n["cls"]]
    b = edits[2]["b"]
    tap_xy((b[0] + b[2]) // 3, (b[1] + b[3]) // 2)
    nap(0.5)
    sh("input keyevent KEYCODE_MOVE_END")
    sh("input keyevent " + " ".join(["KEYCODE_DEL"] * 10))
    nap(0.3)
    fill(0, od.replace(".", SEP))
    fill(1, wall.replace(".", SEP))
    fill(2, radius)
    nap(settle)  # A tube joins the history only after 2 s without changes.


def run_locale(name):
    global SEP
    locale, values, marking, location, count = LOCALES[name]
    SEP = "." if name == "en-US" else ","
    s = strings(values)
    d = os.path.join(OUT, name)
    print(name, flush=True)

    sh(f"am force-stop {PKG}")
    sh(f"pm clear {PKG}")
    sh(f"cmd locale set-app-locales {PKG} --locales {locale}")
    sh(f"am start -n {PKG}/.MainActivity")
    nap(3)

    def refocus(dest="nav_calculator"):
        """Leaving the tab and coming back drops the text cursor from the fields."""
        hide_ime()
        tap(s["nav_balls"])
        tap(s[dest])
        nap(0.5)

    # Ball set. Right after launch the first tap can be lost, so repeat it until the tab opens.
    for _ in range(5):
        tap(s["nav_balls"])
        nap(1)
        if find(s["range_open"]):
            break
    tap(s["range_open"])
    fill(0, "30")
    fill(1, "45")
    tap(s["range_add"])
    nap(1)
    hide_ime()
    size32 = s["ball_size"].replace("%1$s", "32")
    if not find(size32):
        swipe(1500, 1000)
    tap(size32)
    fill(0, marking)
    fill(1, location)
    hide_ime()
    tap(s["dialog_save"])
    nap(1)

    # Tubes, so that the history row has entries
    tap(s["nav_calculator"])
    enter_tube("33.7", "4.5", "60")
    enter_tube("48.3", "5", "70")
    refocus()
    to_top()
    wait_text(s["result_label"])
    shot(os.path.join(d, "1_result.png"))

    # Calculation and table
    for _ in range(6):
        n = find(s["table_footnote"])
        if n and n["b"][3] < 1800:
            break
        swipe(1500, 800)
    shot(os.path.join(d, "2_table.png"))
    to_top()

    # Balls tab with the calculated tube
    tap(s["nav_balls"])
    to_top()
    if not find(s["ball_badge_fits"]):
        swipe(1500, 1150)
    wait_text(s["ball_badge_recommended"])
    shot(os.path.join(d, "3_balls.png"))
    to_top()
    tap(s["nav_calculator"])

    # Compact bar above the keyboard
    to_top()
    edits = [n for n in dump() if "EditText" in n["cls"]]
    tap_xy(*center(edits[2]))
    sh("input keyevent KEYCODE_MOVE_END")
    # Retyping the last digit hides the cursor handle that a tap leaves over the helper text.
    sh("input keyevent KEYCODE_DEL")
    sh("input text 0")
    nap(2)
    if not ime_shown():
        raise RuntimeError("keyboard did not open")
    shot(os.path.join(d, "5_kbd.png"))
    hide_ime()

    # Extrapolated result
    enter_tube("63", "4", "200")
    refocus()
    to_top()
    wait_text(s["result_extrapolated"])
    shot(os.path.join(d, "4_extrapolated.png"))

    if count > 5:
        enter_tube("48.3", "5", "70")
        refocus()
        to_top()
        sh("cmd uimode night yes")
        nap(3)
        shot(os.path.join(d, "6_dark.png"))
        tap(s["nav_info"])
        nap(1)
        shot(os.path.join(d, "7_info.png"))
        sh("cmd uimode night no")
        nap(2)


def find_adb():
    for env in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        sdk = os.environ.get(env)
        if sdk:
            path = os.path.join(sdk, "platform-tools", "adb.exe" if os.name == "nt" else "adb")
            if os.path.exists(path):
                return path
    found = shutil.which("adb")
    if not found:
        sys.exit("adb not found: set ANDROID_HOME or put platform-tools on PATH")
    return found


def pick_device(serial, allow_device):
    out = subprocess.run([ADB, "devices"], capture_output=True, text=True).stdout
    devices = [l.split()[0] for l in out.splitlines()[1:] if l.strip().endswith("device")]
    serial = serial or os.environ.get("ANDROID_SERIAL")
    if serial is None:
        if len(devices) != 1:
            sys.exit(f"Choose a device with --serial, connected: {devices or 'none'}")
        serial = devices[0]
    if serial not in devices:
        sys.exit(f"{serial} is not connected, connected: {devices}")
    if not serial.startswith("emulator-") and not allow_device:
        sys.exit(f"{serial} is not an emulator; the script clears the app's data. Use --allow-device to run it anyway.")
    return serial


def main():
    global ADB, SERIAL
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("locales", nargs="*", default=list(LOCALES), choices=list(LOCALES))
    parser.add_argument("--serial", help="device to use, e.g. emulator-5554 (default: ANDROID_SERIAL or the only device)")
    parser.add_argument("--allow-device", action="store_true", help="also run on a real phone (its app data is cleared)")
    parser.add_argument("--copy", action="store_true", help="replace the screenshots in fastlane/metadata")
    parser.add_argument("--teardown", action="store_true", help="only restore the device settings")
    args = parser.parse_args()
    ADB = find_adb()
    SERIAL = pick_device(args.serial, args.allow_device)
    if args.teardown:
        teardown()
        return
    setup()
    try:
        for name in args.locales:
            run_locale(name)
    finally:
        teardown()
    if args.copy:
        for name in args.locales:
            target = os.path.join(FASTLANE, name, "images", "phoneScreenshots")
            for f in sorted(os.listdir(os.path.join(OUT, name))):
                shutil.copyfile(os.path.join(OUT, name, f), os.path.join(target, f))
            print("copied to", os.path.relpath(target, ROOT))


if __name__ == "__main__":
    main()
