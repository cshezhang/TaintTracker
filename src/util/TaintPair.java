package util;

import soot.Value;

import java.util.HashSet;

public class TaintPair {

    private Value callerParameter;
    private HashSet<Value> taints;

    public TaintPair(Value callerParameter) {
        this.callerParameter = callerParameter;
        this.taints = new HashSet<>();
    }

    public Value getCallerParameter() {
        return callerParameter;
    }

    public HashSet<Value> getTaints() {
        return taints;
    }

    public void addTaint(Value taint) {
        this.taints.add(taint);
    }

    public void addTaints(HashSet<Value> taints) {
        this.taints.addAll(taints);
    }

    public void setTaints(HashSet<Value> taints) {
        this.taints = taints;
    }
}
