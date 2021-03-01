package io.harness.callgraph.instr.tracer;

import io.harness.callgraph.util.StackNode;

import net.bytebuddy.asm.Advice;

/*
  with this setting: suppress = Throwable.class
  we are swallowing any exception caused by instrumentation. The advantage is: we don't crash any unit test at the price
  of generating partial callgraph, and the disadvantage is we won't know if there is any issue with instrumentation.
  Refer : PR-20641
 */
public class MethodTracer {
  @Advice.OnMethodEnter(inline = false, suppress = Throwable.class)
  public static StackNode enter(
      @Advice.Origin("#t") String type, @Advice.Origin("#m") String method, @Advice.Origin("#s") String signature) {
    return CallableTracer.enter(type, method, signature, false);
  }

  @Advice.OnMethodExit(inline = false, onThrowable = Throwable.class, suppress = Throwable.class)
  public static void exit(@Advice.Enter StackNode node) {
    CallableTracer.exit(node);
  }
}