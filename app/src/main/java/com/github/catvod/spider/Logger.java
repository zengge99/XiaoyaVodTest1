package com.github.catvod.spider;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;

public class Logger {
    static boolean dbg = true;

    public static void log(Object message) {
        if (!dbg) {
            return;
        }
        String callPrefix = "";
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length >= 2) {
            StackTraceElement caller = stackTrace[1];
            String className = caller.getClassName();
            String methodName = caller.getMethodName();
            int lineNumber = caller.getLineNumber();
            callPrefix = String.format("Log (called from %s.%s at line %d): ", className, methodName, lineNumber);
        }
        String loggerMessage = "";
        if (String.class.isInstance(message)) {
            loggerMessage = callPrefix + message;
        } else {
            loggerMessage = callPrefix + (new Gson()).toJson(message);
        }
        String filePath = "/storage/emulated/0/TV/log.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(loggerMessage);
            writer.newLine();
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
}
