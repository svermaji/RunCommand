rem %1 is to pass additional parameter to mvn cmd like -o for offline
rem mvn exec:java -D"exec.mainClass"="com.sv.runcmd.RunCommand" -D"exec.args"="'d:\java-prg\ScreenCheck\run.bat'" %1
mvn exec:java -Dexec.mainClass="com.sv.runcmd.RunCommand" -Dexec.args="'*c:\sv\cmds\search.bat (search)'" %1
