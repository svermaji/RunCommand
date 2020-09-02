package com.sv.runcmd;

import javax.swing.*;

public class AppException extends RuntimeException {

    AppException(String msg) {
        super(msg);
    }

    AppException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
