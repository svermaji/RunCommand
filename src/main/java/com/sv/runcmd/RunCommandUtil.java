package com.sv.runcmd;

import com.sv.core.Utils;
import com.sv.core.logger.MyLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RunCommandUtil {

    private final MyLogger logger;
    private final static int RETRY_ATTEMPTS = 3;

    public RunCommandUtil(MyLogger logger) {
        this.logger = logger;
    }

    public boolean runCommands(String[] cmds) {

        logger.log("Running commands " + Arrays.asList(cmds));

        boolean rsp = true;
        List<String> successCmds = new ArrayList<>();
        String failedCmd = "";

        int cmdsLength = cmds.length;
        int skippedIndex = cmdsLength;
        for (int i = 0; i < cmdsLength; i++) {
            String cmd = cmds[i];
            boolean cmdState = runCommand(cmd);
            if (!cmdState) {
                failedCmd = cmd;
                rsp = commandFailed(cmd);
                skippedIndex = i + 1;
                break;
            }
            successCmds.add(cmd);
        }
        logger.log("skippedIndex is [" + skippedIndex + "]");
        List<String> skippedCmds = new ArrayList<>(Arrays.asList(cmds).subList(skippedIndex, cmdsLength));

        logger.log("Commands run successfully " + successCmds + ", skipped commands " + skippedCmds +
                " and failed command is [" + failedCmd + "]");

        return rsp;
    }

    private boolean commandFailed(String cmd) {
        String errMsg = "[" + cmd + "] command FAILED. Any remaining commands will be skipped.";
        logger.error(errMsg);
        return false;
    }

    public boolean runCommand(String cmd) {
        return runCommand(cmd, 1);
    }

    public boolean runCommand(String cmd, int attempt) {

        logger.log("Attempt [" + attempt + "/" + RETRY_ATTEMPTS + "] for command [" + cmd + "]");
        Process process;
        String errStreamData;
        boolean success = true;
        try {
            String cmdStr = cmd;
            String argStr = "";
            if (cmd.contains(" ")) {
                int idx = cmd.indexOf(" ");
                cmdStr = cmd.substring(0, idx);
                argStr = cmd.substring(idx + 1);
            }
            logger.log("Command [" + cmdStr + "] and arg [" + argStr + "]");
            process = Utils.runProcess(new String[]{cmdStr, argStr}, logger);
            errStreamData = Utils.getStreamOutput(process.getErrorStream(), logger);
            if (Utils.hasValue(errStreamData)) {
                success = false;
                logger.error("Error stream data [" + errStreamData + "]");
            }
        } catch (Exception e) {
            success = false;
            if (attempt == RETRY_ATTEMPTS) {
                logger.error("Error in running command " + e.getMessage() + "]. Details: ", e);
            } else {
                logger.error("Error in running command " + e.getMessage());
            }
        }

        if (success && attempt < RETRY_ATTEMPTS) {
            logger.error("Attempt failed for cmd [" + cmd + "], retrying...");
            attempt++;
            success = runCommand(cmd, attempt);
        }

        return success;
    }

}

