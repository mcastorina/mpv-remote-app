# Design
This document aims to explain the design of mpv-remote-app for anyone curious.

## Server

## Message Protocol
mpv-remote-app uses an HMAC with a preshared key to ensure message
authenticity and integrity. Every message excluding "health" is
wrapped in this HMAC in the following format:

```
{
    "message": "{\"time\": 123, \"command\": \"hi\"}",
    "hmac": "89e9d274c49e7bd7a58778208df4d51d"
}
```

## Commands
The messages exchanged between server and client are a few generic white-listed commands:

| command       | required args         | optional args                     |
| ------------- | --------------------- | --------------------------------- |
| health        |                       |                                   |
| play          | path (string)         |                                   |
| pause         | state (boolean)       |                                   |
| stop          |                       |                                   |
| seek          | seconds (integer)     |                                   |
| set_volume    | volume (integer)      |                                   |
| set_subtitles | track (integer)       |                                   |
| set_audio     | track (integer)       |                                   |
| fullscreen    | state (boolean)       |                                   |
| mute          | state (boolean)       |                                   |
| repeat        | args (string array)   | delay (float), speedup (boolean)  |
| list          | directory (string)    |                                   |
| show          | property (string)     | pre (string), post (string)       |

### Examples
```
health

{
    "command": "play",
    "path": "path/to/file.mkv"
}

{
    "command": "pause",
    "state": true
}

{ "command": "stop" }

{
    "command": "seek",
    "seconds": 5
}

{
    "command": "set_volume",
    "volume": 5
}

{
    "command": "set_subtitles",
    "track": 1
}

{
    "command": "set_audio",
    "track": 1
}

{
    "command": "fullscreen",
    "state": true
}

{
    "command": "mute",
    "state": false
}

{
    "command": "repeat",
    "delay": 0.1,
    "speedup": true,
    "args": [
        "seek",
        "seconds",
        "5"
    ]
}

{
    "command": "list",
    "directory": "."
}

{
    "command": "show",
    "property": "volume",
    "pre": "Volume: ",
    "post": "%"
}
```
