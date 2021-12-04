rem expecting all dependecies in same root folder
set dir=%CD%
cd ../Core-Parent
call mvn install -DskiptTests=true
cd ../Core
call mvn install -DskiptTests=true
cd ../SwingUI
call mvn install -DskiptTests=true
cd %dir%
call mvn install -DskiptTests=true
