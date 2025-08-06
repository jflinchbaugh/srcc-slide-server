# SRCC Slide Server

## Setup
- Install the tools:
  - `babashka`
  - `rclone`
  - `eom` (Eye of Mate)
- Interactively configure `eom` to show random images
  and show each for 7 seconds in slideshow.
- Add a remote to `rclone` pointing to the SRCC Google Drive
  and call the remote 'srcc-gdrive'
- Create directories for the different shows in `$HOME/Pictures`
  - `logo` containing the logo only.
  - `default` containing the default ad images it'll show in event mode
    if it started with no connection.
  - `events` which will receive `default` images plus images from the website.
  - `gdrive` which will receive images from the Google Drive.
  - `show` symlink to `events` to default to that on power-on.
- Copy `slide-server.bb` into the home directory.
- Setup the desktop environment
  to autostart `bb slide-server.bb 'Event Room'` upon login.
- Once configured, set the Raspberry Pi to use an Overlay FS,
  so it can always be reliably started into a known state 
  after a power-outageb.
