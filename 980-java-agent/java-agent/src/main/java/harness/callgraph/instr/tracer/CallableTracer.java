package harness.callgraph.instr.tracer;

import harness.callgraph.boot.*;
import harness.callgraph.boot.TraceDispatcher;
import harness.callgraph.instr.CallRecorder;
import harness.callgraph.util.Format;
import harness.callgraph.util.StackNode;

public class CallableTracer extends TraceDispatcher {
  @Override
  public Object enter(String type, String method, String signature, boolean testMethod) {
    signature = Format.simplifySignatureArrays(signature);

    StackNode node = new StackNode(type, method, signature, testMethod);
    CallRecorder.beforeMethod(node);
    return node;
  }

  @Override
  public void exit(Object node) {
    CallRecorder.afterMethod((StackNode) node);
  }
}