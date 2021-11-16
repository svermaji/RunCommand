package com.sv.runcmd.helpers;

import com.sv.runcmd.RunCommandUI;

import java.util.TimerTask;

public class AppFontChangerTask extends TimerTask {

    private final RunCommandUI rc;

    public AppFontChangerTask(RunCommandUI rc) {
        this.rc = rc;
    }

    @Override
    public void run() {
        rc.changeAppFont();
    }
}
