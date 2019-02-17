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
| tracks        |                       |                                   |

### Examples
<pre><table>
    <tr>
        <th>Input</th>
        <th>Output</th>
    </tr>

    <tr>
        <td>health</td>
        <td>{
    "action": null,
    "message": null,
    "result": true,
    "time": 123
}</td>
    </tr>

    <tr>
        <td>{
    "command": "play",
    "path": "path/to/file.mkv",
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "pause",
    "state": true,
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "stop",
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "seek",
    "seconds": 5,
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    <tr>

    <tr>
        <td>{
    "command": "set_volume",
    "volume": 95,
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "set_subtitles",
    "track": 1,
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "set_audio",
    "track": 1,
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "fullscreen",
    "state": true,
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "mute",
    "state": false,
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "repeat",
    "delay": 0.1,
    "speedup": true,
    "args": [
        "seek",
        "seconds",
        "5"
    ]
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "list",
    "directory": ".",
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": {
        "directories": [
            "videos"
        ],
        "files": [
            "outkast-roses.mp3"
        ]
    }
    "result": true,
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "show",
    "property": "volume",
    "pre": "Volume: ",
    "post": "%",
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": null,
    "result": "",
    "time": 124
}</td>
    </tr>

    <tr>
        <td>{
    "command": "tracks",
    "time": 123
}</td>
        <td>{
    "action": 123,
    "message": {
        "audio": [
            "1: eng",
            "2: jpn"
        ],
        "subtitle": [
            "1: jpn"
        ]
    },
    "result": true,
    "time": 124
}</td>
    </tr>
</table></pre>
