# mpv-remote-app
An android application to send UDP commands to `mpv` on my computer,
which is running a python script listening for commands.

<p align="center">
<img src="https://raw.githubusercontent.com/mcastorina/mpv-remote-app/master/screenshots/app-main.png" width="336">
<img src="https://raw.githubusercontent.com/mcastorina/mpv-remote-app/master/screenshots/app-settings.png" width="336">
</p>

## Features

* play / pause
* fast forward / rewind
* volume slider
* fullscreen
* subtitles
* file browsing

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

## Getting Started

### Installation

1. Clone this repository.

```
git clone https://github.com/mcastorina/mpv-remote-app
```

2. Install the python requirements

```
pip install -r requirements.txt
```

3. Install the Android application

- Plug in your Android phone
- Enable developer mode

```
adb install snapshot.apk
```

### Run
Running the server will automatically connect to an existing mpv instance
or spawn a new one if none found. Use `Ctrl+C` to kill the server.

```
python server.py p4ssw0rd --no-hidden
```

### Connect
1. Start the Android app and open the settings
2. Fill in the target IP address to the one the server is running on
3. Fill in the password to the password specified on the command line (e.g. `p4ssw0rd`)
4. Browse and play!

## Motivation
A learning experience writing Android applications and to avoid getting
up to control the movie I am watching.

## Dependencies

* Android (android-tools)
* Python 3
  * psutil

## TODO

* Read / write directly to IPC socket
* Library browsing
    * Caching
    * Save path
* Saved configurations
* Auto find the server on the network
