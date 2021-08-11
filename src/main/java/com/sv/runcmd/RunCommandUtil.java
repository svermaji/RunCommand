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
            int cmdState = runCommand(cmd);
            if (cmdState != 0) {
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

    public int runCommand(String cmd) {
        return runCommand(cmd, 1);
    }

    public int runCommand(String cmd, int attempt) {

        logger.log("Attempt [" + attempt + "/" + RETRY_ATTEMPTS + "] for command [" + cmd + "]");
        int exitVal;
        try {
            String cmdStr = cmd;
            String argStr = "";
            if (cmd.contains(" ")) {
                int idx = cmd.indexOf(" ");
                cmdStr = cmd.substring(0, idx);
                argStr = cmd.substring(idx + 1);
            }
            logger.log("Command [" + cmdStr + "] and arg [" + argStr + "]");

            Process process = Utils.runProcess(new String[]{cmdStr, argStr}, logger);
            exitVal = process.waitFor();
            process.destroyForcibly();
            logger.log("exit value [" + exitVal + "], State [" + getExitState(exitVal) + "].");
            if (!isExitStateSuccess(exitVal)) {
                logger.error("Error is [" + getDetails(process) + "]");
            }
        } catch (InterruptedException e) {
            if (attempt == RETRY_ATTEMPTS) {
                logger.error("Command interrupted " + e.getMessage() + ". Details: ", e);
            } else {
                logger.error("Command interrupted " + e.getMessage());
            }
            exitVal = -1;
        } catch (Exception e) {
            if (attempt == RETRY_ATTEMPTS) {
                logger.error("Error in running command " + e.getMessage() + ". Details: ", e);
            } else {
                logger.error("Error in running command " + e.getMessage());
            }
            exitVal = -1;
        }

        if (!isExitStateSuccess(exitVal) && attempt < RETRY_ATTEMPTS) {
            logger.error("Attempt failed for cmd [" + cmd + "], retrying...");
            attempt++;
            exitVal = runCommand(cmd, attempt);
        }

        return exitVal;
    }

    private String getDetails(Process process) {
        return Utils.getProcessOutput(process, logger);
    }

    private String getExitState(int exitVal) {
        return isExitStateSuccess(exitVal) ? "success" : "failure";
    }

    private boolean isExitStateSuccess(int exitVal) {
        return (0 == exitVal);
    }

}

