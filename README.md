# RunCommand
Utility in Java to run command like batch file or sh file or any other command

## Description<br>
* For date wise changes refer Details-README.md
* This program give UI and commands from a configuration
* In conf.config favourite button limit can be defined as 5 or 10
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
Initial - Attaching screen shot of application:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image.png) 

2-Sep-2020 - Attaching screen shot, now user can have themes and colors:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-theme-color.png) 

2-Sep-2020 - Attaching screen shot, showing colors:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-theme-color-2.png) 

3-Sep-2020 - Attaching screen shot, showing colors:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-fav.png) 

4-Sep-2020 - Attaching screen shot, new look, bring filter closer to table and some UI changes:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-new-ui.png) 

11-Sep-2020 - Attaching screen shot, favourite title:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-fav-2.png) 

22-Sep-2020 - Attaching screen shot, rounded border and bold font:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-images/app-image-bold.png) 
