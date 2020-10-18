package com.sv.runcmd.helpers;

import com.sv.runcmd.RunCommandUI;

import java.util.TimerTask;

public class ColorChangerTask extends TimerTask {

    private final RunCommandUI rc;

    public ColorChangerTask(RunCommandUI rc) {
        this.rc = rc;
    }

    @Override
    public void run() {
        if (Boolean.parseBoolean(rc.getRandomColors())) {
            rc.changeColor();
        }
    }
}
