Guide Me: Android App
=====================

Guide Me is my graduate project in SFSU. The project includes an Android app, a login server, and a video call server. This repo is the Android app.

**Features**

* Register as a visual impairer or a helper; login.
* Visual impairer can open a "room" (i.e. a video call request); waiting for a helper to join.
* Visual impairer can accept (i.e. start the video call) or decline (i.e. keep waiting for another helper).
* Helper can fetch current open room list; enter one of them.
* Helper can navigate the visual impairer on a Google Map (under developing).
* They can rate each other (under developing).
* Friends list (under developing).
* Helping history (under developing).

**Use**

Run the login server, put "url:port" in `Config.LOGIN_SERVER_ADDRESS`

Run the video server, put "url:port" in `Config.VIDEO_SERVER_ADDRESS`

Build and run the app.

**Other components**

* [Login Server](https://github.com/ZTGeng/login-server)
* [Video Server](https://github.com/ZTGeng/video-server)

