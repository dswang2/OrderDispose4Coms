package com.dbstar.crashcanary.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CrashLogs implements Serializable {

    private List<CrashCause> causes = new ArrayList<>();

    public void add(CrashCause cause) {
        causes.add(cause);
    }

    public List<CrashCause> getCauses() {
        return causes;
    }

    @Override
    public String toString() {
        return "CrashLogs{" +
                "causes=" + causes +
                '}';
    }
}