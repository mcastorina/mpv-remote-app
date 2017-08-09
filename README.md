# mpv-remote-app
An android application to send UDP commands to `mpv` on my computer,
which is running a python script listening for commands.

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

## Motivation
A learning experience writing Android applications and to avoid getting
up to control the movie I am watching.

## Dependencies

* Android (android-tools)
* Python 3

## TODO

* Read / write directly to IPC socket
* Getting started section
* Screenshots / APK snapshots
* Library browsing
    * Caching
    * Hidden files option
    * Filetype filter
    * Save path
    * Close current mpv session before starting a new one
* Quit button
* Saved sessions
* Auto find the server on the network
