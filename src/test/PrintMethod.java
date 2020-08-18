package test;

import config.Config;
import soot.*;
import soot.util.Chain;
import util.CG_Generator;

public class PrintMethod {

    public static void main(String[] args) {
        Config.appPath = "E:\\app-debug.apk";
        CG_Generator.constructCallGraph();
        String callerClass = "com.example.test.MainActivity";
        String callerMethod = "change3";
        SootClass targetClass = null;
        Chain<SootClass> classes = Scene.v().getApplicationClasses();
        for (SootClass sc : classes) {
            if (sc.getName().equals(callerClass)) {
                targetClass = sc;
                break;
            }
        }
        SootMethod targetMethod = targetClass.getMethodByName(callerMethod);
        MainTest.printMethod(targetMethod);
    }

}