package util;

import cfg.CFG;
import cfg.Node;
import cfg.Path;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;

import java.util.*;

public class analysis {

    public static HashMap<SootMethod, Boolean> isVisited = new HashMap<>();
    public static HashMap<SootMethod, HashSet<Value>> method2taints = new HashMap<>();

    static class QueueNode {
        public Unit targetUnit;
        public Value value;

        public QueueNode(Unit targetUnit, Value value) {
            this.targetUnit = targetUnit;
            this.value = value;
        }
    }

    public static HashSet<Value> forwardMethodReturnValueAnalysis(SootMethod method, List<Value> callerArgs) {
        if (method.isJavaLibraryMethod() || !method.isConcrete()) {
            return new HashSet<>();
        }
        if(!isVisited.containsKey(method)) {
            isVisited.put(method, true);
        } else {
            //防止出现自环
            HashSet<Value> results = method2taints.get(method);
            if(results == null) {
                return new HashSet<>();
            } else {
                return results;
            }
        }
        CFG.getCFG(method);
        HashSet<Value> results = new HashSet<>();
        Body body = method.retrieveActiveBody();
        Chain<Local> chainLocals = body.getLocals();
        HashMap<Local, HashSet<Value>> getTaints = new HashMap<>();
        for (Local local : chainLocals) {
            getTaints.put(local, new HashSet<>());
        }
        Chain<Unit> chainUnits = body.getUnits();
        for (Unit unit : chainUnits) {
            if(unit instanceof JReturnStmt) {
                Value v = ((JReturnStmt) unit).getOp();
                if(v instanceof JimpleLocal && getTaints.containsKey(v)) {
                    results.addAll(getTaints.get(v));
                }
            }
            if (unit instanceof JAssignStmt) {
                JAssignStmt assignStmt = (JAssignStmt) unit;
                Value leftValue = assignStmt.leftBox.getValue();
                Value rightValue = assignStmt.rightBox.getValue();
                if(leftValue instanceof JInstanceFieldRef) {
                    leftValue = ((JInstanceFieldRef) leftValue).getBase();
                }
                if (chainLocals.contains(leftValue)) {
                    if(rightValue instanceof JimpleLocal) {
                        if(getTaints.containsKey(rightValue)) {
                            getTaints.get(leftValue).addAll(getTaints.get(rightValue));
                        }
                    }
                    if (rightValue instanceof Constant) {
                        getTaints.get(leftValue).add(rightValue);
                        continue;
                    }
                    if (rightValue instanceof StaticFieldRef) {
                        getTaints.get(leftValue).add(rightValue);
                        continue;
                    }
                    if (rightValue instanceof InvokeExpr) {
                        InvokeExpr invokeExpr = (InvokeExpr) rightValue;
                        if(invokeExpr instanceof JVirtualInvokeExpr) {
                            JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr)invokeExpr;
                            Value base = virtualInvokeExpr.getBase();
                            if(chainLocals.contains(base)) {
                                getTaints.get(leftValue).addAll(getTaints.get(base));
                            }
                        }
                        if(invokeExpr instanceof JStaticInvokeExpr) {
                            JStaticInvokeExpr staticInvokeExpr = (JStaticInvokeExpr)invokeExpr;

                        }
                        for(ValueBox vb : invokeExpr.getUseBoxes()) {
                            Value value = vb.getValue();
                            if(value instanceof Local) {
                                Local targetLocal = (Local) value;
                                getTaints.get(leftValue).addAll(getTaints.get(targetLocal));
                                continue;
                            }
                            if(value instanceof StaticFieldRef) {
                                getTaints.get(leftValue).add(value);
                                continue;
                            }
                            if(value instanceof Constant) {
                                getTaints.get(leftValue).add(value);
                            }
                        }
                        if(!invokeExpr.getMethod().isJavaLibraryMethod()
                        && invokeExpr.getMethod().isConcrete()
                        ) {
                            List<Value> argList = invokeExpr.getArgs();
                            TaintPairs taintPairs = new TaintPairs(invokeExpr.getMethod().getName());
                            for (Value v : argList) {
                                if (chainLocals.contains(v)) {
                                    getTaints.get(leftValue).addAll(getTaints.get(v));
                                    TaintPair taintPair = new TaintPair(v);
                                    taintPairs.add(taintPair);
                                }
                            }
                            GlobalCallStack.push(taintPairs);
                            HashSet<Value> returnResults = forwardMethodReturnValueAnalysis(invokeExpr.getMethod(), invokeExpr.getArgs());
                            //处理返回值带来的污染
                            getTaints.get(leftValue).addAll(returnResults);
                            //处理被污染的参数
                            TaintPairs pairsWithProgation = GlobalCallStack.pop();
                            for (TaintPair taintPair : pairsWithProgation.get()) {
                                if (getTaints.containsKey(taintPair.getCallerParameter())) {
                                    getTaints.get(taintPair.getCallerParameter()).addAll(taintPair.getTaints());
                                }
                            }
                        }
                    }
                }
            }
            if (unit instanceof JInvokeStmt) {
                JInvokeStmt invokeStmt = (JInvokeStmt) unit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                SootMethod invokeMethod = invokeExpr.getMethod();
                List<Value> args = invokeExpr.getArgs();
                List<ValueBox> useBoxes = invokeExpr.getUseBoxes();
                if(invokeExpr instanceof JVirtualInvokeExpr) {
                    JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr)invokeExpr;
                    Value base = virtualInvokeExpr.getBase();
                    for(ValueBox vb : useBoxes) {
                        Value value = vb.getValue();
                        if(value instanceof Constant || value instanceof StaticFieldRef) {
                            getTaints.get(base).add(value);
                        }
                    }
                    if(chainLocals.contains(base)) {
                        for(Value arg : virtualInvokeExpr.getArgs()) {
                            if(chainLocals.contains(arg)) {
                                getTaints.get(base).addAll(getTaints.get(arg));
                            }
                        }
                    }
                }
                if (invokeMethod.isConcrete() && !invokeMethod.isJavaLibraryMethod()) {
                    TaintPairs taintPairs = new TaintPairs(invokeExpr.getMethod().getName());
                    for (Value v : args) {
                        if (chainLocals.contains(v)) {
                            TaintPair taintPair = new TaintPair(v);
                            taintPairs.add(taintPair);
                        }
                    }
                    GlobalCallStack.push(taintPairs);
                    forwardMethodReturnValueAnalysis(invokeExpr.getMethod(), invokeExpr.getArgs());
                    TaintPairs pairsWithProgation = GlobalCallStack.pop();
                    for (TaintPair taintPair : pairsWithProgation.get()) {
                        if (getTaints.containsKey(taintPair.getCallerParameter())) {
                            getTaints.get(taintPair.getCallerParameter()).addAll(taintPair.getTaints());
                        }
                    }
                }
            }
        }
        Unit lastUnit = chainUnits.getLast();
        int cnt_locals = 0;
        if (lastUnit instanceof JReturnStmt) {
            for (ValueBox valueBox : lastUnit.getUseBoxes()) {
                if (valueBox.getValue() instanceof Constant) {
                    results.add(valueBox.getValue());
                }
                if (valueBox.getValue() instanceof Local) {
                    cnt_locals++;
                }
            }
        }
        if (GlobalCallStack.size() > 0) {
            TaintPairs taintPairs = GlobalCallStack.peek();
            for (TaintPair taintPair : taintPairs.get()) {
                int index = -1;
                for (int i = 0; i < callerArgs.size(); i++) {
                    if (callerArgs.get(i).equivTo(taintPair.getCallerParameter())) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    continue;
                }
                for (Map.Entry<Local, HashSet<Value>> entry : getTaints.entrySet()) {
                    if (entry.getKey().getName().contains("r" + (index + 1))) {
                        taintPair.addTaints(entry.getValue());
                    }
                }
            }
        }
        if (cnt_locals > 0) {
            Local lastLocal = (Local) lastUnit.getUseBoxes().get(0).getValue();
            results.addAll(getTaints.get(lastLocal));
            method2taints.put(method, results);
            return getTaints.get(lastLocal);
        } else {
            method2taints.put(method, results);
            return results;
        }
    }

    /*
    在一个方法内给定Local和Unit作为起点来进行后向污点分析
     */
    public static HashSet<Value> backwardTaintAnalysis(SootMethod method, Unit initUnit, Value initValue) {
        HashSet<Value> results = new HashSet<>();
        if (method.isJavaLibraryMethod() || !method.isConcrete()) {
            return results;
        }
        if(!isVisited.containsKey(method)) {
            isVisited.put(method, true);
        } else {
            results = method2taints.get(method); //防止出现自环
            if(results == null) {
                return new HashSet<>();
            } else {
                return results;
            }
        }
        if (!(initValue instanceof JimpleLocal)) {
            results.add(initValue);
            method2taints.put(method, results);
            return results;
        }
        CFG cfg = CFG.getCFG(method);
        List<Path> allPathes = cfg.getAllPaths();
        System.out.println("AllPath Size=" + allPathes.size());
        ArrayList<Path> backPathes = new ArrayList<>();
        for(Path path : allPathes) {
            if(path.hasNode(initUnit)) {
                backPathes.add(path);
            }
        }
        System.out.println("back_path_size=" + backPathes.size());
//        Chain<Unit> units = method.retrieveActiveBody().getUnits();
        for(Path path : backPathes) {
            ArrayList<Unit> units = path.getBackwardSlice(initUnit);
//            for(Node node : path.nodes) {
//                units.add(node.unit);
//            }
            ArrayDeque<QueueNode> que = new ArrayDeque<>();
            QueueNode headNode = new QueueNode(initUnit, initValue);
            que.addLast(headNode);
            while (!que.isEmpty()) {
                QueueNode node = que.pollFirst();
                Unit prevUnit = null;
                //这里应该有个双向链表实现更简单，但是考虑到units不会很大，也不会浪费时间，就省内存了
                for(int i = 1; i < units.size(); i++) {
                    if(units.get(i).equals(node.targetUnit)) {
                        prevUnit = units.get(i - 1);
                    }
                }
                while (prevUnit != null) {
                    List<ValueBox> defBoxes = prevUnit.getDefBoxes();
                    for (ValueBox valueBox : defBoxes) {
                        if (valueBox.getValue() instanceof JimpleLocal && valueBox.getValue().equivTo(node.value)) {
                            //找到last define并分析
                            if (prevUnit instanceof JAssignStmt) {
                                JAssignStmt assignStmt = (JAssignStmt) prevUnit;
                                Value rightValue = assignStmt.rightBox.getValue();
                                if (rightValue instanceof InvokeExpr) {
                                    InvokeExpr invokeExpr = (InvokeExpr) rightValue;
                                    SootMethod callee = invokeExpr.getMethod();
                                    if (!callee.isJavaLibraryMethod() && callee.isConcrete()) {
                                        HashSet<Value> reValues = forwardMethodReturnValueAnalysis(callee, invokeExpr.getArgs());
                                        results.addAll(reValues);
                                    }
                                }
                            }
                            if (prevUnit instanceof JIdentityStmt) {
                                JIdentityStmt identityStmt = (JIdentityStmt) prevUnit;
                                Value rightValue = identityStmt.rightBox.getValue();
                                results.add(rightValue);
                                if (rightValue instanceof ParameterRef) {
                                    ParameterRef parameterRef = (ParameterRef) rightValue;
                                    CallGraph cg = Scene.v().getCallGraph();
                                    Iterator<Edge> edges = cg.edgesInto(method);
                                    while (edges.hasNext()) {
                                        Edge e = edges.next();
                                        SootMethod caller = e.getSrc().method();
                                        Unit callerUnit = e.srcUnit();
                                        int argIndex = parameterRef.getIndex();
                                        InvokeExpr invokeExpr = null;
                                        if (callerUnit instanceof JInvokeStmt) {
                                            invokeExpr = ((JInvokeStmt) callerUnit).getInvokeExpr();
                                        }
                                        if (callerUnit instanceof JAssignStmt) {
                                            invokeExpr = (InvokeExpr) ((JAssignStmt) callerUnit).rightBox.getValue();
                                        }
                                        Value callerPara = invokeExpr.getArg(argIndex);
                                        HashSet<Value> callerTaints = backwardTaintAnalysis(caller, callerUnit, callerPara);
                                        results.addAll(callerTaints);
                                    }
                                }
                            }
                            //最后这里加一些常数分析放到Taints里边，之前的主要是做过程间分析
                            List<ValueBox> useBoxes = prevUnit.getUseBoxes();
                            for (ValueBox vb : useBoxes) {
                                Value value = vb.getValue();
                                if (value instanceof StaticFieldRef) {
                                    results.add(value);
                                    continue;
                                }
                                if (value instanceof Constant) {
                                    results.add(value);
                                    continue;
                                }
                                if (value instanceof JimpleLocal) {
                                    QueueNode newNode = new QueueNode(prevUnit, value);
                                    que.addLast(newNode);
                                }
                            }
                            //这个unit为lastDefine，我们把右端的污染local全部放到队列里后结束prevUnit的访问
                            continue;
                        }
                    }
                    if (prevUnit instanceof JInvokeStmt) {
                        JInvokeStmt invokeStmt = (JInvokeStmt) prevUnit;
                        InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                        if (invokeExpr instanceof JVirtualInvokeExpr) {
                            JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr) invokeExpr;
                            Value base = virtualInvokeExpr.getBase();
                            List<Value> args = virtualInvokeExpr.getArgs();
                            String name = virtualInvokeExpr.getMethod().getName();
                            if (name.equals("appendQueryParameter") || name.equals("append") || name.equals("putInt") ||
                                    name.equals("putDouble") || name.equals("putLong") ||
                                    name.equals("putString")) { //这里还需要考虑更深层调用的问题
                                if (base instanceof JimpleLocal && base.equivTo(node.value)) {
                                    for (Value arg : args) {
                                        if (arg instanceof Constant) {
                                            results.add(arg);
                                        }
                                        //virtualinvoke $r2.<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>($r3);
                                        if (arg instanceof JimpleLocal) {
                                            que.addLast(new QueueNode(prevUnit, arg));
//                                        HashSet<Value> localTaints = backwardTaintAnalysis(method, prevUnit, arg);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if(prevUnit.equals(units.get(0))) {
                        prevUnit = null;
                    } else {
                        for (int i = 1; i < units.size(); i++) {
                            if (units.get(i).equals(prevUnit)) {
                                prevUnit = units.get(i - 1);
                            }
                        }
                    }
                }
            }
        }
        method2taints.put(method, results);
        return results;
    }

    public static HashSet<Value> backwardAnalysis(SootMethod method, String calleeClass,
                                                  String calleeMethod, List<String> calleeArgsList,
                                                  int para_index) {
        HashSet<Value> results = new HashSet<>();
        isVisited.clear();
        method2taints.clear();
        Body body = method.retrieveActiveBody();
        if (body == null) {
            System.err.println("This method doesn't have active body! Please report to Wayne.Zhang. :)");
            return results;
        }
        Chain<Unit> units = body.getUnits();
        ArrayList<Unit> slicingUnits = new ArrayList<>();
        Unit calleeUnit = null;
        InvokeExpr invokeExpr = null;
        for (Unit unit : units) {
            //Search for the callee.
            slicingUnits.add(unit);
            if (unit instanceof JInvokeStmt) {
                JInvokeStmt invokeStmt = (JInvokeStmt)unit;
                InvokeExpr invoke = invokeStmt.getInvokeExpr();
                if(invoke.getMethod().getDeclaringClass().getName().equals(calleeClass)
                    && invoke.getMethod().getName().equals(calleeMethod)) {
                    calleeUnit = unit; //这里还没有加入用Args来匹配，不过目前还没有发现问题，待开发
                    invokeExpr = ((JInvokeStmt) unit).getInvokeExpr();
                    break;
                }
            }
            if (unit instanceof JAssignStmt) {
                Value rightValue = ((JAssignStmt) unit).rightBox.getValue();
                if (rightValue instanceof InvokeExpr) {
                    InvokeExpr testInvoke = (InvokeExpr) rightValue;
                    if (testInvoke.getMethod().getName().equals(calleeMethod) &&
                            testInvoke.getMethod().getDeclaringClass().getName().equals(calleeClass)) {
                        invokeExpr = (InvokeExpr) rightValue;
                        calleeUnit = unit;
                        break;
                    }
                }
            }
        }
        if (calleeUnit == null) {
            System.err.println("Not find the callee! Please check the callee info or report to Wayne.Zhang. :)");
            System.err.println("Callee Info: " + calleeClass + "->" + calleeMethod);
            return results;
        }
        Local targetLocal;
        Value testValue = invokeExpr.getArg(para_index);
//        System.out.println("Target Unit=" + targetUnit);
//        System.out.println("Target Class=" + targetUnit.getClass());
        if (testValue instanceof JimpleLocal) {
            targetLocal = (JimpleLocal) testValue;
//            System.out.println("Target Local=" + targetLocal.getName());
        } else {
            results.add(testValue);
            return results;
        }
        results.addAll(backwardTaintAnalysis(method, calleeUnit, targetLocal));
        return results;
    }


    //通过字符串搜索目标Class，貌似有Soot自带的api，但我忘记了。。回头再搞
    public static SootClass getSootClass(String className) {
        SootClass targetClass = null;
        Chain<SootClass> classes = Scene.v().getApplicationClasses();
        for (SootClass sc : classes) {
            if (sc.getName().equals(className)) {
                targetClass = sc;
                break;
            }
        }
        return targetClass;
    }

    //如果参数为空就直接传null
    public static SootMethod getMethod(SootClass sc, String methodName, List<String> argsList) {
        SootMethod result = null;
        List<SootMethod> methods = sc.getMethods();
        for (SootMethod sootMethod : methods) {
            if (sootMethod.getName().equals(methodName)) {
                if (argsList != null) {
                    boolean tag = true;
                    List<Type> targetArgs = sootMethod.getParameterTypes();
                    int len1 = argsList.size(), len2 = targetArgs.size();
                    if (len1 != len2) {
                        continue;
                    }
                    for (int i = 0; i < len1; i++) {
                        if (!argsList.get(i).equals(targetArgs.get(i).toString())) {
                            tag = false;
                            break;
                        }
                    }
                    if (!tag) {
                        continue;
                    }
                }
                result = sootMethod;
                break;
            }
        }
        return result;
    }

}
