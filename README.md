# RunCommand
Utility in Java to run command like batch file or sh file or any other command

## Description<br>
* For date wise changes refer Details-README.md
* Auto Lock feature supported based on config
* Lock screen and change password (with salt) screen added
* Update for color and app checkbox
* Update for recent labels for quick filter
* This program give UI and commands from a configuration
* Added context menu to copy command from a row
* Button to open config folder and command prompt (on Windows only)
* In conf.config favourite button limit can be defined as 5 or 10
* Settings menu added for selecting different colors and LookAndFeels and Timer close command
* Recent filters menu added
* Settings menu is now a bar
* Filter text box will be saved from last session and reapplied.
* Recent filters applied will come as auto complete.
* Indexing on favourite buttons can be controlled by configuration
* User can use script to kill task which is image of cmd.exe or java.exe (No need to create batch file for that)
* User can either double-click using mouse or press enter key to run selected command
* Command can be foreground or background, cmd screen may or may not visible
* If there are large number of commands, user can use filter
* At runtime "Reload" button can be used to refresh commands
    - Example: If batch file called as `start BATCHFILE` then process will be in foreground
    - Example: If batch file called as `BATCHFILE` then process will be in background
* Different log file for different modes (UI/API)
    - Create API version of same, now either call single command from `cmd.exe` or use UI
    - For how to use API refer `run-arg.bat`
* Tooltips are available to guide
* Most prominent actions can used as favourites (max 5), even at runtime by clicking reload.  User just need to add `*` at the start of command.
    - If fav commands are less or more, rest will be ignored
* If name is long it will be trimmed (See image)
* On startup window will be set to right most part of screen
* Last run command will be displayed in title bar and in information label at top
* User can select `random themes` and `random colors` option that will change every 10 minutes
* On setting off `random themes` and `random colors` default will be reset - attached two screen shots

#### Images<br>
* Application Present Image<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-3-oct-21.png) 

* Application Present Menu Image<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-3-oct-21-menu.png)

* Application colored Image<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-colored-3-oct-21.png)

* Lock Screen Image<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-lock-1-oct-21.png)

* Change Password Image<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-change-pwd-1-oct-21.png)

* First cut<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-first-cut.png) 

* Settings - Colors menu<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-colors-menu.png) 

* Settings - Themes menu<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-themes-menu.png) 

* Filter<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-filter.png) 

* 10 Favourites<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-10-favs.png) 

