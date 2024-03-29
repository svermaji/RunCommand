package com.sv.runcmd;

import com.sv.core.Utils;
import com.sv.core.logger.MyLogger;

import java.util.Arrays;

public class RunCommand {

    private MyLogger logger;
    private final RunCommandUtil commandUtil;

    public static void main(String[] args) {
        new RunCommand(args);
    }

    public RunCommand(String[] args, MyLogger loggerObj) {

        this.logger = loggerObj;
        if (args != null && args.length == 1) {
            if (logger == null) {
                this.logger = MyLogger.createLogger("run-cmd-arg.log");
            }
            commandUtil = new RunCommandUtil(logger);
            commandUtil.execCommand(args[0]);
            Utils.sleep1Sec();
        } else {
            if (loggerObj == null) {
                this.logger = MyLogger.createLogger("run-cmd.log");
            }
            commandUtil = new RunCommandUtil(logger);
            new RunCommandUI(logger);
        }
    }

    public RunCommand(String[] args) {
        this(args, null);
    }
}

