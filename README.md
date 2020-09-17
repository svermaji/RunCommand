# RunCommand
Utility in Java to run command like batch file or sh file or any other command

## Recent Changes<br>
#### On 17-Sep-2020<br>
* Create API version of same, now either call single command from `cmd.exe` or use UI
* For how to use API refer `run-arg.bat`

#### On 11-Sep-2020<br>
* Added title for favourite buttons
* Fixed few bugs

#### On 10-Sep-2020<br>
* Disable/Enable controls between running command to avoid multiple runs
* Update for UI

#### On 03-Sep-2020<br>
* Bring filter closer to table, and some UI changes
* Tooltips are better now

#### On 03-Sep-2020<br>
* Now favourite buttons added (max 5)
* Most prominent actions can used, even at runtime by clicking reload.  User just need to add `*` at the start of command.
* If fav commands are less or more, rest will be ignored
* Tooltip and Enable will be done accordingly
* If name is long it will be trimmed (See image)

#### On 02-Sep-2020<br>
* On startup window will be set to right most part of screen
* Filter can be cleared with button and short key action
* Last run command will be displayed in title bar and in information label at top
* User can select `random themes` and `random colors` option that will change every 10 minutes
* On setting off `random themes` and `random colors` default will be reset - attached two screen shots

#### On 01-Sep-2020<br>
Initial check in.<br>

## Description<br>
This program give UI and commands from a configuration are present in tabular format<br>
User can either double-click using mouse or press enter key to run that command<br>
Command can be foreground or background, cmd screen may or may not visible<br>
If there are more commands user can use filter<br>
At runtime "Reload" button can be used to refresh commands<br>
* Example: If batch file called as `start BATCHFILE` then process will be in foreground<br>
* Example: If batch file called as `BATCHFILE` then process will be in background<br>

#### Images<br>
Initial - Attaching screen shot of application:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-image.png) 

2-Sep-2020 - Attaching screen shot, now user can have themes and colors:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-image-theme-color.png) 

2-Sep-2020 - Attaching screen shot, showing colors:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-image-theme-color-2.png) 

3-Sep-2020 - Attaching screen shot, showing colors:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-image-fav.png) 

4-Sep-2020 - Attaching screen shot, new look, bring filter closer to table and some UI changes:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-image-new-ui.png) 

11-Sep-2020 - Attaching screen shot, favourite title:<br>
![Image of Yaktocat](https://github.com/svermaji/RunCommand/blob/master/app-image-fav-2.png) 
