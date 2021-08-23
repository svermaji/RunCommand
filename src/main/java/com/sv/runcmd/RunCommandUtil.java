package com.sv.runcmd;

import com.sv.core.Utils;
import com.sv.core.logger.MyLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
            Utils.runProcess(new String[]{cmdStr, argStr}, logger);
        } catch (Exception e) {
            success = false;
            if (attempt == RETRY_ATTEMPTS) {
                logger.error("Error in running command " + e.getMessage() + "]. Details: ", e);
            } else {
                logger.error("Error in running command " + e.getMessage());
            }
        }

        if (!success && attempt < RETRY_ATTEMPTS) {
            logger.error("Attempt failed for cmd [" + cmd + "], retrying...");
            attempt++;
            success = runCommand(cmd, attempt);
        }

        return success;
    }

    private String chopStar(String cmd) {
        if (cmd.startsWith("*")) {
            cmd = cmd.substring(1);
        }
        return cmd;
    }

    public String getCmdToRun(String cmd) {
        String chk = " (";
        cmd = cmd.contains(chk) ?
                cmd.substring(0, cmd.indexOf(chk)) : cmd;
        return chopStar(cmd);
    }

    public void execCmdAsProcess(String cmd) {
        String cmdStr = getCmdToRun(cmd);
        logger.log("Calling command [" + cmdStr + "]");
        Utils.logProcessOutput(Utils.runProcess(cmdStr, logger), logger);
    }

    public String execCommand(String cmd) {
        String cmdStr = getCmdToRun(cmd);
        logger.log("Calling command [" + cmdStr + "]");

        if (isPidCmd(cmdStr)) {
            tracePid(cmdStr);
        } else {
            return runCommand(cmdStr) + "";
        }

        return "";
    }

    private boolean isPidCmd(String cmd) {
        String fn = Utils.chopFileNameExtn(Utils.getFileName(cmd));
        return fn.startsWith("pid-");
    }

    /**
     * It is in recursion for execCommand
     *
     * @param cmd command
     */
    private void tracePid(String cmd) {
        String processName = Utils.chopFileNameExtn(Utils.getFileName(cmd));
        processName = processName.substring(processName.indexOf("pid-") + "pid-".length());

        String tlCmd = getCmdForCmdPrompt(processName);
        String pid = getPidFor(tlCmd, processName);

        if (!Utils.hasValue(pid)) {
            tlCmd = getCmdForJava(processName);
            pid = getPidFor(tlCmd, processName);
        }

        if (Utils.hasValue(pid)) {
            execCmdAsProcess(getKillCmdFor(pid));
        }
    }

    private String getPidFor(String cmd, String processName) {
        Process process = Utils.runProcess(cmd, logger);
        assert process != null;

        String line, pidString = "", pid = "";
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader stdError = new BufferedReader(new
                     InputStreamReader(process.getErrorStream()))) {

            StringBuilder sb = new StringBuilder();
            do {
                line = reader.readLine();
                if (Utils.hasValue(line)) {
                    sb.append(line);
                    if (captureData(line, processName)) {
                        pidString = line;
                    }
                }
            } while (line != null);

            logger.log("PID String captured as " + pidString + " from output: " + sb);

            if (!Utils.hasValue(pidString)) {
                sb = new StringBuilder();
                logger.warn("No data from process. Checking error stream.");
                do {
                    line = stdError.readLine();
                    if (Utils.hasValue(line)) {
                        sb.append(line);
                    }
                } while (line != null);
                if (!Utils.hasValue(sb.toString())) {
                    logger.warn("No error stream data.");
                } else {
                    logger.warn("Error stream data: " + sb);
                }
            } else {
                // Array must come as output of nearly 9 elements, each encloses in ""
                pid = pidString.split(",")[1];
                if (Utils.hasValue(pid) && pid.length() > 2) {
                    pid = pid.substring(1, pid.length() - 1);
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }

        return pid;
    }

    private boolean captureData(String line, String processName) {
        String lnLC = line.toLowerCase();
        return !lnLC.contains("tasklist")
                && !lnLC.contains("imagename")
                && lnLC.contains("running")
                && lnLC.contains(processName)
                ;
    }

    private String getCmdForCmdPrompt(String cmd) {
        return getListCmdFor("cmd.exe", cmd);
    }

    private String getCmdForJava(String cmd) {
        return getListCmdFor("java.exe", cmd);
    }

    private String getListCmdFor(String process, String cmd) {
        return ".\\cmds\\tl.bat " + process + " " + cmd;
    }

    private String getKillCmdFor(String process) {
        return ".\\cmds\\tk.bat " + process;
    }
}

