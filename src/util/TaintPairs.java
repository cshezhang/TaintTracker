package util;

import java.util.ArrayList;

public class TaintPairs {

    private String methodName;
    private ArrayList<TaintPair> taintPairs;

    public TaintPairs(String methodName) {
        this.methodName = methodName;
        this.taintPairs = new ArrayList<>();
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void add(TaintPair taintPair) {
        this.taintPairs.add(taintPair);
    }

    public ArrayList<TaintPair> get() {
        return this.taintPairs;
    }

}
