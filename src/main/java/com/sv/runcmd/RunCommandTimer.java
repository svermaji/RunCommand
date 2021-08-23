package com.sv.runcmd;

import com.sv.core.logger.MyLogger;

import java.util.TimerTask;

public class RunCommandTimer extends TimerTask {

    private final MyLogger logger;
    private final RunCommandUtil commandUtil;

    public RunCommandTimer(MyLogger logger, RunCommandUtil commandUtil) {
        this.logger = logger;
        this.commandUtil = commandUtil;
    }

    @Override
    public void run() {

    }
}

