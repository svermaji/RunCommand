package com.sv.runcmd;

import com.sv.core.Utils;
import com.sv.core.logger.MyLogger;

public class RunCommand {

    private final MyLogger logger;
    private final RunCommandUtil commandUtil;

    public static void main(String[] args) {
        new RunCommand(args);
    }

    public RunCommand(String[] args) {
        if (args != null && args.length == 1) {
            logger = MyLogger.createLogger("run-cmd-arg.log");
            commandUtil = new RunCommandUtil(logger);
            commandUtil.execCommand(args[0]);
            Utils.sleep1Sec();
        } else {
            logger = MyLogger.createLogger("run-cmd.log");
            commandUtil = new RunCommandUtil(logger);
            new RunCommandUI(logger);
        }
    }
}

