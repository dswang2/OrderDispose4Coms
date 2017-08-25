package com.dbstar.crashcanary.model;

import java.io.Serializable;

public class CrashCause implements Serializable {

    private String stackTrace;

    public CrashCause(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public String toString() {
        return "CrashCause{" +
                "stackTrace='" + stackTrace + '\'' +
                '}';
    }
}
