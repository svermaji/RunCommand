rem %1 is to pass additional parameter to mvn cmd like -o for offline
mvn exec:java -D"exec.mainClass"="com.sv.runcmd.RunCommand" %1