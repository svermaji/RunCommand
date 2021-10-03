# RunCommand
Utility in Java to run command like batch file or sh file or any other command

## Recent Changes<br>
#### On 3-Oct-2021<br>
* Auto Lock feature supported based on config
* Option to see full command or only name

#### On 1-Oct-2021<br>
* Lock screen and change password screen added
* Password is securely stored with salt

#### On 30-Sep-2021<br>
* Update for recent labels for quick filter

#### On 28-Sep-2021<br>
* Update for color and app checkbox

#### On 25-Aug-2021<br>
* Settings menu have more items
* Timer for close command
* Recent filters added

#### On 1-Dec-2020<br>
* Settings menu is on top bar now

#### On 25-Nov-2020<br>
* Added settings menu

#### On 8-Oct-2020<br>
* Indexing on favourite buttons can be controlled by configuration

#### On 7-Oct-2020<br>
* In conf.config favourite button limit can be defined as 5 or 10

#### On 22-Sep-2020<br>
* UI Fixes, used rounded border which changes color as information label does.  Made information font bolder and bigger
* Different log file for different mode (UI/API)
* Created Details-README.md

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
