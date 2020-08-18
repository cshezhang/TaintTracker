package util;

import java.util.Stack;

public class GlobalCallStack {

    public static Stack<TaintPairs> taintStack = new Stack<>();

    public static void push(TaintPairs taintPairs) {
        taintStack.push(taintPairs);
    }

    public static int size() {
        return taintStack.size();
    }

    public static TaintPairs peek() {
        return taintStack.peek();
    }

    public static TaintPairs pop() {
        return taintStack.pop();
    }
}
