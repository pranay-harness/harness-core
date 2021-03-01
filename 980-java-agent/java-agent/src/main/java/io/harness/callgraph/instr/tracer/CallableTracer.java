package io.harness.callgraph.instr.tracer;

import io.harness.callgraph.instr.CallRecorder;
import io.harness.callgraph.util.Format;
import io.harness.callgraph.util.StackNode;

public abstract class CallableTracer {
  public static StackNode enter(String type, String method, String signature, boolean testMethod) {
    signature = Format.simplifySignatureArrays(signature);

    StackNode node = new StackNode(type, method, signature, testMethod);
    CallRecorder.beforeMethod(node);
    return node;
  }

  public static void exit(StackNode node) {
    CallRecorder.afterMethod(node);
  }
}