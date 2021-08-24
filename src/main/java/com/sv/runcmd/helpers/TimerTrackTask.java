package com.sv.runcmd.helpers;

import com.sv.runcmd.RunCommandUI;

import java.util.TimerTask;

public class TimerTrackTask extends TimerTask {

    private final RunCommandUI rc;

    public TimerTrackTask(RunCommandUI rc) {
        this.rc = rc;
    }

    @Override
    public void run() {
        rc.trackTimer();
    }
}
