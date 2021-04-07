package harness.callgraph.instr.tracer;

import harness.callgraph.boot.TraceDispatcher;
import net.bytebuddy.asm.Advice;

/*
  with this setting: suppress = Throwable.class
  we are swallowing any exception caused by instrumentation. The advantage is: we don't crash any unit test at the price
  of generating partial callgraph, and the disadvantage is we won't know if there is any issue with instrumentation.
  Refer : PR-20641
 */
public class MethodTracer {
  @Advice.OnMethodEnter(inline = true, suppress = Throwable.class)
  public static Object enter(
      @Advice.Origin("#t") String type, @Advice.Origin("#m") String method, @Advice.Origin("#s") String signature) {
    return TraceDispatcher.INSTANCE.enter(type, method, signature, false);
  }

  @Advice.OnMethodExit(inline = true, onThrowable = Throwable.class, suppress = Throwable.class)
  public static void exit(@Advice.Enter Object node) {
    TraceDispatcher.INSTANCE.exit(node);
  }
}