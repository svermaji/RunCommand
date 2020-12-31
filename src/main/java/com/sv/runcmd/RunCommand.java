package com.sv.runcmd;

import com.sv.core.Utils;
import com.sv.core.logger.MyLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunCommand {

    private final MyLogger logger;

    public static void main(String[] args) {
        new RunCommand(args);
    }

    public RunCommand(String[] args) {
        if (args != null && args.length == 1) {
            logger = MyLogger.createLogger("run-cmd-arg.log");
            execCommand(args[0]);
            Utils.sleep1Sec();
        } else {
            logger = MyLogger.createLogger("run-cmd.log");
            new RunCommandUI(this, logger);
        }
    }

    private String chopStar(String cmd) {
        if (cmd.startsWith("*")) {
            cmd = cmd.substring(1);
        }
        return cmd;
    }

    private String getCmdToRun(String cmd) {
        String chk = " (";
        cmd = cmd.contains(chk) ?
                cmd.substring(0, cmd.indexOf(chk)) : cmd;
        return chopStar(cmd);
    }

    public String execCommand(String cmd) {
        String cmdStr = getCmdToRun(cmd);
        logger.log("Calling command [" + cmdStr + "]");

        if (isPidCmd(cmdStr)) {
            tracePid(cmdStr);
        } else {
            return Utils.runCmd(cmdStr, logger);
        }

        return "";
    }

    private boolean isPidCmd(String cmd) {
        String fn = Utils.chopFileNameExtn(Utils.getFileName(cmd));
        return fn.startsWith("pid-");
    }

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
            execCommand(getKillCmdFor(pid));
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

            do {
                line = reader.readLine();
                if (Utils.hasValue(line)) {
                    logger.log(line);
                }
                if (line != null && captureData(line, processName)) {
                    pidString = line;
                }
            } while (line != null);

            logger.log("PID String captured as " + pidString);

            if (!Utils.hasValue(pidString)) {
                logger.warn("No data from process. Checking error stream.");
                do {
                    line = stdError.readLine();
                    if (Utils.hasValue(line)) {
                        logger.warn(line);
                    }
                } while (line != null);
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

