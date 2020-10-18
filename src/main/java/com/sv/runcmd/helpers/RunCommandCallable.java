package com.sv.runcmd.helpers;

import com.sv.runcmd.RunCommandUI;

import java.util.concurrent.Callable;

public class RunCommandCallable implements Callable<Boolean> {

    private final RunCommandUI rc;
    private final String cmd;

    public RunCommandCallable(RunCommandUI rc, String command) {
        this.rc = rc;
        this.cmd = command;
    }

    @Override
    public Boolean call() {
        rc.runCmdCallable (cmd);
        return true;
    }
}
