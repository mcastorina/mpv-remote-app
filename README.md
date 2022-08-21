# mpv-remote-app
An Android application to control the [mpv media player](https://mpv.io/)
running on a separate computer.

<p align="center">
<img src="https://raw.githubusercontent.com/mcastorina/mpv-remote-app/master/screenshots/app-main.png" width="336">
<img src="https://raw.githubusercontent.com/mcastorina/mpv-remote-app/master/screenshots/app-settings.png" width="336">
</p>

## Features

* play / pause / stop
* fast forward / rewind
* volume slider
* fullscreen
* subtitles toggle
* audio / subtitle selection
* file browsing
* automatically find server
* NEW! play yt video streams from URL or search keywords

## How it Works
`mpv` has an option `--input-ipc-server` which allows you to control
the state of the media player, so the python script `server.py` acts as
a mediator between the unix socket specified to `mpv` and the Android
application. It will receive commands via UDP packets and translate it
into a form the `mpv` IPC server understands.

HMAC with a shared secret is used to ensure message authenticity and
integrity, so only users with the server's secret can alter its state.
Because the server receives commands over the network, this means it is
possible, although a very bad idea, to open this up to the Internet. I
do not recommend it.

For a more detailed description, refer to [DESIGN.md](DESIGN.md).

## Getting Started

### Installation

1. Install the server (Python 3 only)

```
python3 -m pip install mpv-remote-app
```

2. Install the Android application

The Android application is [available on F-Droid
here](https://f-droid.org/en/packages/miccah.mpvremote/).

Alternatively, you can follow these steps to install the APK manually
(requires android tools):

- Download [snapshot.apk](snapshot.apk)
- Plug in your Android phone
- Enable developer mode

```
adb install snapshot.apk
```

### Run
Running the server will automatically connect to an existing mpv instance
or spawn a new one if none found. Use `Ctrl+C` to kill the
server. Currently, the server only works with Python 3.

You can manually start mpv below or let the server spawn it for you.
```
mpv --input-ipc-server=/tmp/mpvsocket --idle
```

```
mpv-remote-app p4ssw0rd
```

### Connect
1. Start the Android app and open the settings
2. Fill in the target IP address to the one the server is running on
3. Fill in the password to the password specified on the command line (e.g. `p4ssw0rd`)
4. Browse and play!

## Dependencies

* Android (android-tools)
* Python 3
  * psutil
