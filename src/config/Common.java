package config;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Common {
    public static List<String> excludeList = new LinkedList<String>(Arrays.asList(
        "java.*",
        "sun.*",
        "android.*",
        "androidx.*",
        "org.apache.*",
        "org.eclipse.*",
        "soot.*",
        "javax.*",
        "jdk.*"
    ));

    public static List<String> targetList = new LinkedList<String>(Arrays.asList(
        "com.example"
    ));

    public static List<String> potentialBypassApis = new LinkedList<>(Arrays.asList(
            "<java.lang.Object: java.lang.Class getClass()>",
            "<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>"
    ));
}
