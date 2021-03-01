package io.harness.callgraph.boot;

public abstract class TraceDispatcher {

    public static volatile TraceDispatcher INSTANCE;

    public abstract Object enter(String type, String method, String signature, boolean testMethod);

    public abstract void exit(Object node);
}
