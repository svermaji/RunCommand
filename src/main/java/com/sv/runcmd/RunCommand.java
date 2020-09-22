package com.sv.runcmd;

import java.io.IOException;

public class RunCommand {

    private final MyLogger logger;

    public static void main(String[] args) {
        new RunCommand(args);
    }

    public RunCommand(String[] args) {
        if (args != null && args.length == 1) {
            logger = MyLogger.createLogger("run-cmd-arg.log");
            execCommand(args[0]);
            Utils.sleep(1000);
        } else {
            logger = MyLogger.createLogger("run-cmd.log");
            new RunCommandUI(this, logger);
        }
    }

    private String chopStar(String cmd) {
        if (cmd.startsWith("*")) {
            cmd = cmd.substring(1);
        }
        return cmd;
    }

    private String getCmdToRun(String cmd) {
        String chk = " (";
        cmd = cmd.contains(chk) ?
                cmd.substring(0, cmd.indexOf(chk)) : cmd;
        return chopStar(cmd);
    }

    public void execCommand(String cmd) {
        String cmdStr = getCmdToRun(cmd);
        logger.log("Calling command [" + cmdStr + "]");
        try {
            Runtime.getRuntime().exec(cmdStr);
        } catch (IOException e) {
            logger.error(e);
        }
    }
}

