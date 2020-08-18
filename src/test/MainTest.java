package test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import config.Config;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;
import util.CG_Generator;
import util.analysis;

import java.io.*;
import java.util.*;

public class MainTest {

    public static final boolean log = false;

    public static String pkgName;
    public static String cgname;
    public static ArrayList<String> tags = new ArrayList<>();
    public static ArrayList<String> callerClasses = new ArrayList<>();
    public static ArrayList<String> callerMethods = new ArrayList<>();
    public static ArrayList<List<String>> callersArgList = new ArrayList<>();
    public static ArrayList<String> calleeClasses = new ArrayList<>();
    public static ArrayList<String> calleeMethods = new ArrayList<>();
    public static ArrayList<List<String>> calleesArgList = new ArrayList<>();
    public static ArrayList<String> sensitiveCalleeClass = new ArrayList<>();
    public static ArrayList<String> sensitiveCalleeMethod = new ArrayList<>();
    public static ArrayList<String> sensitiveCalleeArgLists = new ArrayList<>();

    public static void main(String[] args) throws IOException, XmlPullParserException {
        //初始化传入的参数
        if (args.length == 6) {
            System.out.println("Target JSON Path=" + args[0]); //json path
            System.out.println("Android SDK Platform Path=" + args[1]); //platform path
            System.out.println("Target APK Path=" + args[2]); //apk path
            System.out.println("AndroidCallbacks Path=" + args[3]);
            System.out.println("CHA Path=" + args[4]);
            Config.androidPlatformPath = args[1];
            Config.appPath = args[2];
            Config.callbacksPath = args[3];
            CG_Generator.CHA_PATH = args[4];
            cgname = args[5];
            parseJSON(args[0]);
        } else if (args.length == 5) {
            System.out.println("Android SDK Platform Path=" + args[0]); //platform path
            System.out.println("Target APK Path=" + args[1]); //apk path
            System.out.println("AndroidCallbacks Path=" + args[2]);
            System.out.println("CHA Path=" + args[3]);
            Config.androidPlatformPath = args[0];
            Config.appPath = args[1];
            Config.callbacksPath = args[2];
            CG_Generator.CHA_PATH = args[3];
            cgname = args[4];
        }
        ProcessManifest manifest = new ProcessManifest(Config.appPath);
        pkgName = manifest.getManifest().getAttribute("package").toString();
        pkgName = pkgName.substring(9, pkgName.length() - 1);
        System.out.println("Package Name=" + pkgName);
        CG_Generator.constructCHAList();
        CG_Generator.constructCallGraph();
        initSensitiveAPIs();
        DotGraph dotGraph = new DotGraph(cgname);
        printCG(dotGraph, Scene.v().getCallGraph());  //打印Call Graph
        for (int i = 0; i < callerClasses.size(); i++) {
            String callerClass = callerClasses.get(i);
            String callerMethod = callerMethods.get(i);
            List<String> callerArgsList = transformParameterList(callersArgList.get(i));
            String calleeClass = calleeClasses.get(i);
            String calleeMethod = calleeMethods.get(i);
            List<String> calleeArgsList = transformParameterList(calleesArgList.get(i));
            SootClass targetClass = analysis.getSootClass(callerClass);
            System.out.println("--------Current Analyzing Functions--------");
            System.out.println("Caller: ->" + callerClass + " ->" + callerMethod + " ->" + callerArgsList);
            System.out.println("Callee: ->" + calleeClass + " ->" + calleeMethod + " ->" + calleeArgsList);
            if (targetClass == null) {
                System.out.println("Unable to find the class!");
                continue;
            }
//            System.out.println("Found Caller Class: ->" + targetClass.getName());
//            List<SootMethod> methods = targetClass.getMethods();
//            System.out.println("----Methods belong to the class----");
//            for(SootMethod method : methods) {
//                System.out.println(method.getName());
//                System.out.println(method.getParameterTypes());
//            }

//            System.out.println("Found Caller Method: ->" + targetMethod.getName());
            SootMethod targetMethod = analysis.getMethod(targetClass, callerMethod, callerArgsList);
            HashSet<Value> taints = new HashSet<>();
            HashSet<String> fcm_res = new HashSet<>();
//            printMethod(targetMethod);
            taints.addAll(analysis.backwardAnalysis(targetMethod, calleeClass, calleeMethod, calleeArgsList, 0));
            printTaints(taints, tags.get(i));
            System.gc();
        }
        /*
        Chain<SootClass> classes = Scene.v().getApplicationClasses();
        for(SootClass srcClass : classes) {
            for(SootMethod srcMethod : srcClass.getMethods()) {
                if(!srcMethod.isConcrete() || srcMethod.isJavaLibraryMethod()) {
                    continue;
                }
                Body body = srcMethod.retrieveActiveBody();
                if (body == null) {
                    continue;
                }
                Chain<Unit> units = body.getUnits();
                Unit targetUnit = null;
                InvokeExpr invokeExpr = null;
                for (Unit unit : units) {
                    boolean find = false;
                    if (unit instanceof JInvokeStmt) {
                        JInvokeStmt invokeStmt = (JInvokeStmt) unit;
                        invokeExpr = invokeStmt.getInvokeExpr();
                        if (healthCalleeClass.contains(invokeExpr.getMethod().getDeclaringClass().getName())
                                && healthCalleeMethod.contains(invokeExpr.getMethod().getName())) {
                            find = true;
                            targetUnit = unit;
                            invokeExpr = ((JInvokeStmt) unit).getInvokeExpr();
                        }
                    }
                    if (unit instanceof JAssignStmt) {
                        Value rightValue = ((JAssignStmt) unit).rightBox.getValue();
                        if (rightValue instanceof InvokeExpr) {
                            invokeExpr = (InvokeExpr) rightValue;
                            if (healthCalleeClass.contains(invokeExpr.getMethod().getDeclaringClass().getName())
                                && healthCalleeMethod.contains(invokeExpr.getMethod().getName())) {
                                find = true;
                                targetUnit = unit;
                            }
                        }
                    }
                    if(find) {
                        String callerClass = srcClass.getName();
                        String callerMethod = srcMethod.getName();
                        String calleeClass = invokeExpr.getMethod().getDeclaringClass().getName();
                        String calleeMethod = invokeExpr.getMethod().getName();
                        SootMethod targetMethod = invokeExpr.getMethod();
                        System.out.println("--------Current Analyzing Functions--------");
                        System.out.println("Caller: ->" + callerClass + " ->" + callerMethod + " ->" + srcMethod.getParameterTypes());
                        System.out.println("Callee: ->" + calleeClass + " ->" + calleeMethod + " ->" + targetMethod.getParameterTypes());
//                        printMethod(srcMethod);
                        HashSet<Value> taints;
                        if(targetMethod.getName().equals("insertStepsToGoogle")) {
                            taints = analysis.backwardAnalysis(srcMethod,
                                    calleeClass, calleeMethod, invokeExpr.getArgs(),
                                    1, targetUnit, invokeExpr);
                        } else {
                            taints = analysis.backwardAnalysis(srcMethod,
                                    calleeClass, calleeMethod, invokeExpr.getArgs(),
                                    0, targetUnit, invokeExpr);
                        }
                        if(calleeMethod.equals("addKeyword")) {
                            printTaints(taints, "GoogleAds");
                        } else {
                            printTaints(taints, "Health Data");
                        }
                        System.gc();
                    }
                }
            }
        }
        */
    }

    public static void printCG(DotGraph dot, CallGraph cg) {
        System.out.println("Begin to print the Call Graph...");
        QueueReader<Edge> edges = cg.listener();
        Set<String> visited = new HashSet<>();
        File resultFile = new File("./CallGraphs/" + cgname + ".dot");
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


    //用于分析静态变量在<clinit>函数中的初始化
    public static HashSet<Value> initAnalysis(SootClass targetClass, StaticFieldRef staticValue) {
        HashSet<Value> results = new HashSet<>();
        SootMethod method = analysis.getMethod(targetClass, "<clinit>", null);
        if (method == null) {
            return results;
        }
        if(!method.isConcrete()) {
            return results;
        }
        Chain<Unit> units = method.retrieveActiveBody().getUnits();
        Unit firstDefine = null;
        JimpleLocal targetLocal = null;
        for(Unit unit : units) {
            if(unit instanceof JAssignStmt) {
                Value leftValue = ((JAssignStmt) unit).leftBox.getValue();
                if(leftValue instanceof StaticFieldRef && leftValue.equivTo(staticValue)) {
                    firstDefine = unit;
                    Value rightValue = ((JAssignStmt) unit).rightBox.getValue();
                    if(rightValue instanceof JimpleLocal) {
                        targetLocal = (JimpleLocal) rightValue;
                    }
                    break;
                }
            }
        }

        Unit currentUnit = firstDefine;

        while(currentUnit != null) {
            currentUnit = units.getPredOf(currentUnit);
            if(currentUnit instanceof JInvokeStmt) {
                InvokeExpr invokeExpr = ((JInvokeStmt) currentUnit).getInvokeExpr();
                if(! (invokeExpr instanceof JSpecialInvokeExpr)) {
                    continue;
                }
                JSpecialInvokeExpr specialInvokeExpr = (JSpecialInvokeExpr) invokeExpr;
                if(specialInvokeExpr.getBase().equivTo(targetLocal)) {
//                    System.out.println("Add Stmt=" + currentUnit);
                    for(ValueBox vb : currentUnit.getUseBoxes()) {
                        Value value = vb.getValue();
                        if(value instanceof StringConstant) {
                            results.add(value);
                            continue;
                        }
                        if(value instanceof Constant) {
                            results.add(value);
                            continue;
                        }
                        if(value instanceof StaticFieldRef) {
                            StaticFieldRef staticFieldRef = (StaticFieldRef)value;
                            HashSet<Value> res = initAnalysis(staticFieldRef.getField().getDeclaringClass(), staticFieldRef);
                            results.addAll(res);
                        }
                    }
                }
            }
            if(currentUnit instanceof JAssignStmt) {
                Value leftValue = ((JAssignStmt) currentUnit).leftBox.getValue();
                if(leftValue.equivTo(targetLocal)) {
                    List<ValueBox> useBoxes = currentUnit.getUseBoxes();
                    for(ValueBox vb : useBoxes) {
                        Value value = vb.getValue();
                        if(value instanceof StringConstant) {
                            results.add(vb.getValue());
                            continue;
                        }
                        if(value instanceof Constant) {
                            results.add(vb.getValue());
                            continue;
                        }
                        if(value instanceof StaticFieldRef) {
                            StaticFieldRef staticFieldRef = (StaticFieldRef)value;
                            HashSet<Value> res = initAnalysis(staticFieldRef.getField().getDeclaringClass(), staticFieldRef);
                            results.addAll(res);
                        }
                    }
                    break;
                }
            }
        }
        return results;
    }

    public static void printFCM(HashSet<String> taints, String tag) {
        if(taints.isEmpty()) {
            return;
        }
        System.out.println("--------Taint Values From " + tag + "--------");
        if (!taints.isEmpty()) {
            for (String taint : taints) {
                System.out.println("Value: " + taint);
            }
            System.out.println("------------------------------------------\n");
        }
    }

    public static void printTaints(HashSet<Value> taints, String tag) {
        if(taints.isEmpty()) {
            return;
        }
        System.out.println("--------Taint Values From " + tag + "--------");
        if (!taints.isEmpty()) {
            for (Value taint : taints) {
                if(taint instanceof ParameterRef) {
                    continue;
                }
                if(taint instanceof ThisRef) {
                    continue;
                }
                if(taint == null) {
                    continue;
                }
                System.out.println("Value: " + taint);
//                System.out.println("Class Type: " + taint.getClass());
                if (taint instanceof StaticFieldRef) {
                    StaticFieldRef staticFieldRef = (StaticFieldRef) taint;
                    SootClass declaringClass = staticFieldRef.getField().getDeclaringClass();
                    HashSet<Value> initTaints = MainTest.initAnalysis(declaringClass, (StaticFieldRef) taint);
                    if (initTaints.size() > 0) {
                        System.out.println("====Static Field Init Value====");
                        for (Value v : initTaints) {
                            System.out.println("Value: " + v);
//                            System.out.println(v.getClass());
                        }
                        System.out.println("================================");
                    }

                }
            }
            System.out.println("------------------------------------------\n");
        }
    }

    public static void printMethod(SootMethod method) {
        if (method == null || !method.isConcrete()) {
            return;
        }
        Body body = method.retrieveActiveBody();
        if(body == null) {
            return;
        }
        System.out.println(method.getActiveBody().toString());
        Chain<Unit> units = body.getUnits();
        System.out.println("----Units----");
        for (Unit unit : units) {
            System.out.println(unit.toString());
            System.out.println("Unit Type=" + unit.getClass().toString());
            List<ValueBox> defs = unit.getDefBoxes();
            List<ValueBox> uses = unit.getUseBoxes();
            System.out.println("----Defs----");
            for (ValueBox valueBox : defs) {
                System.out.println("ValueBox=" + valueBox.toString() + "  " + valueBox.getClass().toString());
                System.out.println("Value=" + valueBox.getValue().toString() + "  " + valueBox.getValue().getClass().toString());
            }
            System.out.println("----Uses----");
            for (ValueBox valueBox : uses) {
                System.out.println("ValueBox=" + valueBox.toString() + "   " + valueBox.getClass().toString());
                System.out.println("Value=" + valueBox.getValue().toString() + "  " + valueBox.getValue().getClass().toString());
                if (valueBox.getValue() instanceof InvokeExpr) {
                    InvokeExpr value = (InvokeExpr) valueBox.getValue();
                    System.out.println("InvokeExpr=" + value.toString());
                    System.out.println("Name=" + value.getMethod().getName());
                    System.out.println("Args=" + value.getArgs());
                }
            }
            System.out.println("----End----");
        }
    }

    public static HashSet<SootMethod> searchCallers(SootMethod targetMethod) {
        HashSet<SootMethod> callerMethods = new HashSet<>();
        for(SootClass sc : Scene.v().getApplicationClasses()) {
            for(SootMethod srcMethod : sc.getMethods()) {
                if(srcMethod.isConcrete()) {
                    Body body = srcMethod.retrieveActiveBody();
                    Chain<Unit> units = body.getUnits();
                    for(Unit unit : units) {
                        if(unit instanceof JInvokeStmt) {
                            JInvokeStmt invokeStmt = (JInvokeStmt)unit;
                            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                            SootMethod invokeMethod = invokeExpr.getMethod();
                            if(invokeMethod.equals(targetMethod)) {
                                callerMethods.add(srcMethod);
                                break;
                            }
                        }
                        if(unit instanceof JAssignStmt) {
                            Value rhs = ((JAssignStmt) unit).rightBox.getValue();
                            if(rhs instanceof InvokeExpr) {
                                SootMethod invokeMethod = ((InvokeExpr) rhs).getMethod();
                                if(invokeMethod.equals(targetMethod)) {
                                    callerMethods.add(srcMethod);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return callerMethods;
    }

    public static ArrayList<String> transformParameterList(List<String> paraList) {
//        System.out.println("Source=" + paraList + "  size=" + paraList.size());
        ArrayList<String> res = new ArrayList<>();
        for(int i = 0; i < paraList.size(); i++) {
            StringBuilder ans = null;
            String para = paraList.get(i);
            boolean isArray = false;
            String mainStr;
            if(para.charAt(0) == '[') {
                isArray = true;
                mainStr = para.substring(1);
            } else {
                mainStr = para;
            }
            if(mainStr.length() == 1) {
                if(mainStr.charAt(0) == 'Z') {
                    ans = new StringBuilder("boolean");
                }
                if(mainStr.charAt(0) == 'B') {
                    ans = new StringBuilder("byte");
                }
                if(mainStr.charAt(0) == 'S') {
                    ans = new StringBuilder("short");
                }
                if(mainStr.charAt(0) == 'C') {
                    ans = new StringBuilder("char");
                }
                if(mainStr.charAt(0) == 'I') {
                    ans = new StringBuilder("int");
                }
                if(mainStr.charAt(0) == 'J') {
                    ans = new StringBuilder("long");
                }
                if(mainStr.charAt(0) == 'F') {
                    ans = new StringBuilder("float");
                }
                if(mainStr.charAt(0) == 'D') {
                    ans = new StringBuilder("double");
                }
            } else {
                if(mainStr.charAt(0) == 'L') { //
                    ans = new StringBuilder(mainStr.substring(1, mainStr.length() - 1));
                } else {
                    ans = new StringBuilder(mainStr);
                }
            }
            if(isArray) {
                ans.append("[]");
            }
            res.add(ans.toString());
        }
//        System.out.println(res);
        return res;
    }

    public static void initSenstiveAPIs() {
    }

    public static void parseJSON(String jsonPath) {
        String fileName = jsonPath;
        StringBuilder jsonContent = new StringBuilder();
        try {
            FileReader fr = new FileReader(fileName);
            BufferedReader bf = new BufferedReader(fr);
            String str;
            while ((str = bf.readLine()) != null) {
                jsonContent.append(str);
            }
            bf.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.parseArray(jsonContent.toString());
        if(jsonArray == null) {
            return;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject.containsKey("privacy_api")) {
                JSONArray apiList = (JSONArray) jsonObject.get("privacy_api");
                for (int j = 0; j < apiList.size(); j++) {
                    JSONArray api = apiList.getJSONArray(j);
                    String apiName = api.getString(0);
                    if (apiName.equals("FirebaseAnalytics")
                            || apiName.equals("FCM")
                            || apiName.equals("GoogleAds")
                            || apiName.equals("ContentProvider")
                            || apiName.equals("GoogleAnalytics")
                            || apiName.equals("Fitness")
                    ) {
                        JSONArray apiArray = api.getJSONArray(1);
                        for (int k = 0; k < apiArray.size(); k++) {
                            JSONObject firebase = apiArray.getJSONObject(k);
                            String callerInfo = firebase.getString("method");
                            String calleeInfo = firebase.getString("api");
//                            System.out.println("Method=");
//                            System.out.println(callerInfo);
//                            System.out.println("API=");
//                            System.out.println(calleeInfo);
                            int index1 = callerInfo.indexOf(";->");
                            int index2 = calleeInfo.indexOf(";->");
                            StringBuilder callerClass = new StringBuilder(callerInfo.substring(1, index1));
                            StringBuilder callerMethod = new StringBuilder(callerInfo.substring(index1 + 3));
                            StringBuilder calleeClass = new StringBuilder(calleeInfo.substring(1, index2));
                            StringBuilder calleeMethod = new StringBuilder(calleeInfo.substring(index2 + 3));
                            ArrayList<String> caller = parseMethodSignature(callerMethod);
                            ArrayList<String> callee = parseMethodSignature(calleeMethod);
                            String className1 = parseClassName(callerClass);
                            String className2 = parseClassName(calleeClass);
                            tags.add(apiName);
                            callerClasses.add(className1);
                            calleeClasses.add(className2);
                            callerMethods.add(caller.get(0));
                            calleeMethods.add(callee.get(0));
                            List<String> callerArgs = caller.subList(1, caller.size());
                            callersArgList.add(callerArgs);
                            List<String> calleeArgs = callee.subList(1, callee.size());
                            calleesArgList.add(calleeArgs);
//                            System.out.println(caller.get(0));
//                            System.out.println(callee.get(0));
//                            System.out.println(callerArgs);
//                            System.out.println(calleeArgs);

                        }
                    }
                }
                break;
            }
        }
    }

    public static String parseClassName(StringBuilder name) {
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '/') {
                name.setCharAt(i, '.');
            }
        }
        return name.toString();
    }

    public static ArrayList<String> parseMethodSignature(StringBuilder signature) {
        ArrayList<String> res = new ArrayList<>();
        for (int i = 0; i < signature.length(); i++) {
            if (signature.charAt(i) == '/') {
                signature.setCharAt(i, '.');  //首先替换掉签名中的/为.用来在之后在soot中用
            }
        }
        int index1 = signature.indexOf("("), index2 = signature.indexOf(")");  //获取参数的范围
        res.add(signature.substring(0, index1));
        StringBuilder arg = new StringBuilder();
        for (int i = index1 + 1; i < index2; i++) {
            if (signature.charAt(i) == ' ' || signature.charAt(i) == ')') {
                res.add(arg.toString());
                arg = new StringBuilder();
                continue;
            }
            arg.append(signature.charAt(i));
        }
        if(!arg.toString().equals("")) {
            res.add(arg.toString());
        }
        return res;
    }

}