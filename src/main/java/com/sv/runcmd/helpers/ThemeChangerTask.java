package com.sv.runcmd.helpers;

import com.sv.runcmd.RunCommandUI;

import java.util.TimerTask;

public class ThemeChangerTask extends TimerTask {

    private final RunCommandUI rc;

    public ThemeChangerTask(RunCommandUI rc) {
        this.rc = rc;
    }

    @Override
    public void run() {
        if (Boolean.parseBoolean(rc.getRandomThemes())) {
            rc.changeTheme();
        }
    }
}
