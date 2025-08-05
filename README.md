# SRCC Slide Server

## Setup
- Install
  - `babashka`
  - `rclone`
  - `eom` (Eye of Mate)
- Interactively configure `eom` to show random images
  and show each for 7 seconds in slideshow.
- Add a remote to `rclone` pointing to the SRCC Google Drive
  and call it 'srcc-gdrive'
- Create directories for the different shows in `$HOME/Pictures`
  - `logo` containing the logo only.
  - `default` containing the default ad images it'll show with no connection.
  - `events` which will receive `default` images plus images from the website.
  - `gdrive` which will receive images from the Google Drive.
- Copy `slide-server.bb` into the home directory.
- Setup the desktop environment
  to autostart `bb slide-server.bb "Event Room"` upon login.
