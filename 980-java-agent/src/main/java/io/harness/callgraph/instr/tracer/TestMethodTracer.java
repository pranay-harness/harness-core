package io.harness.callgraph.instr.tracer;

import io.harness.callgraph.util.StackNode;
import io.harness.callgraph.util.log.Logger;

import net.bytebuddy.asm.Advice;

public class TestMethodTracer {
  public static final Logger LOG = new Logger(TestMethodTracer.class);

  @Advice.OnMethodEnter(inline = false)
  public static StackNode enter(
      @Advice.Origin("#t") String type, @Advice.Origin("#m") String method, @Advice.Origin("#s") String signature) {
    return CallableTracer.enter(type, method, signature, true);
  }

  @Advice.OnMethodExit(inline = false, onThrowable = Throwable.class)
  public static void exit(@Advice.Enter StackNode node) {
    CallableTracer.exit(node);
  }
}
