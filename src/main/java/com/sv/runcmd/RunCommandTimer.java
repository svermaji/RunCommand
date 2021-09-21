package com.sv.runcmd;

import com.sv.core.Constants;
import com.sv.core.Utils;
import com.sv.core.logger.MyLogger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class RunCommandTimer extends TimerTask {

    private final MyLogger logger;
    private final RunCommandUI runCommandUI;
    private final String cmd;
    private final LocalDateTime startTime;
    private final long timeLimit;

    public RunCommandTimer(MyLogger logger, RunCommandUI runCommandUI, String cmd, long timeLimit) {
        this.logger = logger;
        this.runCommandUI = runCommandUI;
        this.cmd = cmd;
        this.timeLimit = timeLimit;
        this.startTime = LocalDateTime.now();
    }

    public long getDateTimeDiffSec() {
        long secToMinus = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        return TimeUnit.MILLISECONDS.toSeconds(timeLimit) - secToMinus;
    }

    public String getDateTimeDiff() {
        long sec = getDateTimeDiffSec();
        long min = TimeUnit.SECONDS.toMinutes(sec);
        sec -= TimeUnit.MINUTES.toSeconds(min);
        String time = min + Constants.COLON + (sec > 9 ? sec : "0" + sec);
        logger.debug("Time remaining " + Utils.addBraces(time));
        return time;
    }

    @Override
    public void run() {
        logger.info("Running command from timer as " + Utils.addBraces(cmd));
        runCommandUI.execCommand(cmd);
    }
}

