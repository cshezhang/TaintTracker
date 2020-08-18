package test;

import config.Config;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;
import util.CG_Generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class AnalyzeCG {

    public static String pkgName;
    public static String cg_name;
    public static ArrayList<SootMethod> entry = new ArrayList<>();
    public static ArrayList<String> sensitiveMethods = new ArrayList<>();
    public static ArrayList<String> sensitiveConstants = new ArrayList<>();
    public static HashSet<SootMethod> privacyMethods = new HashSet<>();

    public static void initMethod() {
        sensitiveMethods.add("getDefaultSensor");
        sensitiveMethods.add("getSystemService");
        sensitiveMethods.add("getLastKnowLocation");
        sensitiveMethods.add("getLastKnowLocations");
        sensitiveMethods.add("getLastKnownLocation");
        sensitiveMethods.add("getLastKnownLocations");
        sensitiveMethods.add("getLastLocation");
        sensitiveMethods.add("getLocations");
        sensitiveMethods.add("getLatitude");
        sensitiveMethods.add("getAltitude");
        sensitiveMethods.add("requestLocationUpdates");
        sensitiveMethods.add("onSensorChanged");
        sensitiveMethods.add("onAccuracyChanged");
        sensitiveMethods.add("setAudioSource");
        sensitiveMethods.add("setAudioEncoder");
        sensitiveConstants.add("content://com.android.calendar/calendars");
        sensitiveConstants.add("content://com.android.calendar/events");
        sensitiveConstants.add("content://com.android.calendar/reminders");
        sensitiveConstants.add("content://com.android.contacts/contacts");
        sensitiveConstants.add("android.media.action.IMAGE_CAPTURE");
        sensitiveConstants.add("android.media.action.VIDEO_CAPTURE");
    }

    public static void main(String[] args) throws IOException, XmlPullParserException {

        initMethod();

        if(args.length == 4) {
            Config.androidPlatformPath = args[0];
            Config.appPath = args[1];
            Config.callbacksPath = args[2];
            CG_Generator.CHA_PATH = "E:\\CHA.txt";
            ProcessManifest manifest = new ProcessManifest(Config.appPath);
            pkgName = manifest.getManifest().getAttribute("package").toString();
            pkgName = pkgName.substring(9, pkgName.length() - 1);
            cg_name = args[3];
        } else {
            //服务器并行测试用
            pkgName = args[0].substring(0, args[0].length() - 6);
            Config.androidPlatformPath = "/home/zhe/AndroidSDK/android_sdk/platforms";
            Config.appPath = "/home/zhe/txl_cases/" + pkgName + ".apk";
            Config.callbacksPath = "/home/zhe/AndroidCallbacks.txt";
            CG_Generator.CHA_PATH = "/home/zhe/CHA.txt";
            cg_name = pkgName;
        }

        System.out.println("Package Name=" + pkgName);
        CG_Generator.constructCallGraphByAnalyzingCG();
        Chain<SootClass> classes = Scene.v().getClasses();
        DotGraph dotGraph = new DotGraph("CallGraph");
        printCG(dotGraph, Scene.v().getCallGraph());  //打印Call Graph

        System.out.println("Begin to search privacy method...");
        for(SootClass sc : classes) {
            List<SootMethod> methods = sc.getMethods();
            for(SootMethod method : methods) {
                if(checkSensitiveAPI(method)) {
                    privacyMethods.add(method);
                }
            }
        }
        System.out.println("The Count of Analyzing Target:  " + privacyMethods.size());
        System.out.println("=================Privacy Method to Analyze==================");
        for(SootMethod sootMethod : privacyMethods) {
            System.out.println(sootMethod.getName());
        }
        System.out.println("=========================================================");
        ArrayList<CallPath> results = new ArrayList<>(); //最终结果
        for(SootMethod testMethod : privacyMethods) {
            System.out.println("ClassName=" + testMethod.getDeclaringClass().getName() + "MethodName=" + testMethod.getName());
            System.out.println(testMethod.retrieveActiveBody());
            ArrayList<CallPath> pathes = getCallPath(testMethod); //获取一个方法可以产生的所有Call Path
            System.out.println("Generating " + pathes.size() + " path.");
            for(CallPath path : pathes) {
                System.out.println("-----");
                System.out.println(path);
                System.out.println("-----");
                if(checkPath(path)) {  //判断路径上是否有包名类下的方法
                    results.add(path);
                }
            }
        }
        System.out.println("Size of Last Results=" + results.size());
        for(CallPath callPath : results) {
            System.out.println(callPath);
        }
    }

    public static boolean checkPath(CallPath path) {
        for(SootMethod method : path.path) {  //check是否包含包名方法
            SootClass d_class = method.getDeclaringClass();
            if(d_class.getPackageName().equals(pkgName)) {
                return true;
            }
        }
        return false;
    }

    public static void printCG(DotGraph dot, CallGraph cg) {
        System.out.println("Begin to print the Call Graph...");
        QueueReader<Edge> edges = cg.listener();
        Set<String> visited = new HashSet<>();
        File resultFile = new File("./CallGraphs/" + cg_name + ".dot");
        PrintWriter out = null;
        try {
            out = new PrintWriter(resultFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert out != null;
        out.println("CG begins==================");
        // iterate over edges of the call graph
        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod target = (SootMethod) edge.getTgt();
            MethodOrMethodContext src = edge.getSrc();
            if (!visited.contains(src.toString())) {
                dot.drawNode(src.toString());
                visited.add(src.toString());
            }
            if (!visited.contains(target.toString())) {
                dot.drawNode(target.toString());
                visited.add(target.toString());
            }
            out.println(src + "  -->   " + target);
            dot.drawEdge(src.toString(), target.toString());
        }
        out.println("CG ends==================");
        out.close();
        System.out.println("Call Graph Size=" + cg.size());
        System.out.println("Call Graph has been generated!");
    }

    public static ArrayList<CallPath> getCallPath(SootMethod srcMethod) {
        ArrayList<CallPath> callPathes = new ArrayList<>();
        CallGraph cg = Scene.v().getCallGraph();
        ArrayDeque<CallPath> que = new ArrayDeque<>();
        CallPath initPath = new CallPath(srcMethod);
        que.addLast(initPath);
        System.out.print(srcMethod.getName() + " Begin to Get Methods...");
        while(!que.isEmpty()) {
            CallPath callPath = que.pollFirst();
            SootMethod curMethod = callPath.getLast();
            Iterator<Edge> edges = cg.edgesInto(curMethod);
            ArrayList<SootMethod> srcMethods = new ArrayList<>();
            while(edges.hasNext()) {
                Edge edge = edges.next();
                SootMethod targetMethod = edge.getSrc().method();
                srcMethods.add(targetMethod);
                if(!callPath.hasMethod(targetMethod)) {
                    CallPath newPath = new CallPath(callPath);
                    newPath.addCall(targetMethod);
                    que.addLast(newPath);
                }
            }
            if(srcMethods.size() == 0 || (srcMethods.size() == 1 && srcMethods.get(0).equals(curMethod))) {
                callPathes.add(callPath);
            }
        }
        System.out.println("...End!");
        ArrayList<CallPath> results = new ArrayList<>();
        for(CallPath callPath : callPathes) {
            if(callPath.path.size() > 0 && checkPath(callPath)) {
                results.add(callPath);
            }
        }
        return results;
    }

    public static boolean checkSensitiveAPI(SootMethod method) {
        if(!method.isConcrete() || method.isJavaLibraryMethod()) {
            return false;
        }
        if(method.equals("setNearbyPeople")) {
            return true;
        }
        Body body = method.retrieveActiveBody();
        Chain<Unit> units = body.getUnits();
        for(Unit unit : units) {
            List<ValueBox> valueBoxes = unit.getUseAndDefBoxes();
            for(ValueBox vb : valueBoxes) {
                Value value = vb.getValue();
                if(value instanceof InvokeExpr) {
                    InvokeExpr invokeExpr = (InvokeExpr)value;
                    String invokeName = invokeExpr.getMethod().getName();
                    if(sensitiveMethods.contains(invokeName)) {
                        if(invokeName.equals("getSystemService")) {
                            boolean tag = false;
                            for(ValueBox box : invokeExpr.getUseBoxes()) {
                                Value v = box.getValue();
                                if(v instanceof StringConstant) {
                                    String cString = ((StringConstant) v).value;
                                    if(cString.equals("sensor") || cString.equals("location")) {
                                        tag = true;
                                        break;
                                    }
                                }
                            }
                            if(!tag) { //没有匹配到相应字符串
                                continue;
                            }
                            return true;
                        }
                        return true;
                    }
                }
                if(value instanceof StringConstant) {
                    StringConstant constant = (StringConstant)value;
                    if(sensitiveConstants.contains(constant.value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static class CallPath {

        public SootMethod lastMethod;
        public ArrayList<SootMethod> path;

        public CallPath(SootMethod srcMethod) {
            lastMethod = srcMethod;
            path = new ArrayList<>();
            path.add(srcMethod);
        }

        public CallPath(CallPath rhs) {
            this.lastMethod = rhs.lastMethod;
            this.path = new ArrayList<>();
            this.path.addAll(rhs.path);
        }

        public void addCall(SootMethod sootMethod) {
            lastMethod = sootMethod;
            this.path.add(sootMethod);
        }

        public SootMethod getLast() {
            return lastMethod;
        }

        public boolean hasMethod(SootMethod sootMethod) {
            if(path.contains(sootMethod)) {
                return true;
            }
            return false;
        }

        public boolean containPath(CallPath callPath) {
            if(this.path.size() >= callPath.path.size()) {
                boolean c = true;
                for(SootMethod method : callPath.path) {
                    if(!this.path.contains(method)) {
                        c = false;
                        break;
                    }
                }
                if(c) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();
            res.append("Call Path:\n");
            if(path.size() > 0) {
                for (int i = 0; i < path.size() - 1; i++) {
                    SootMethod method = path.get(i);
                    res.append("(Class=" + method.getDeclaringClass().getName() + "---Method=" + method.getName() + ")--->");
                }
                res.append("(Class=" + lastMethod.getDeclaringClass().getName() + "---Method=" + lastMethod.getName() + ")");
                res.append("\n");
            }
            return res.toString();
        }

    }

}

//                if(!method.isJavaLibraryMethod() || method.isConcrete()) {
//                    Body body = method.retrieveActiveBody();
//                    if(body == null) {
//                        continue;
//                    }
//                    Chain<Unit> units = body.getUnits();
//                    for(Unit unit : units) {
//                        if(unit instanceof JInvokeStmt) {
//                            JInvokeStmt invokeStmt = (JInvokeStmt) unit;
//                            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
//                            if(sensitiveMethods.contains(invokeExpr.getMethod().getName())) {
//                                privacyMethods.add(method);
//                                privacyUnits.add(unit);
//                            }
//                        }
//                        if(unit instanceof JAssignStmt) {
//                            JAssignStmt assignStmt = (JAssignStmt) unit;
//                            Value leftValue = assignStmt.rightBox.getValue();
//                            if(leftValue instanceof InvokeExpr) {
//                                InvokeExpr invokeExpr = (InvokeExpr)leftValue;
//                                if(sensitiveMethods.contains(invokeExpr.getMethod().getName())) {
//                                    privacyMethods.add(method);
//                                    privacyUnits.add(unit);
//                                }
//                            }
//                        }
//                        for(ValueBox vb : unit.getUseBoxes()) {
//                            Value v = vb.getValue();
//                            if(v instanceof StringConstant && sensitiveConstants.contains(((StringConstant) v).value)) {
//                                privacyMethods.add(method);
//                                privacyUnits.add(unit);
//                            }
//                        }
//                    }
//                }
