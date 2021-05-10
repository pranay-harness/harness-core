package harness.callgraph.instr.tracer;

import harness.callgraph.boot.TraceDispatcher;
import net.bytebuddy.asm.Advice;

public class TestMethodTracer {
  @Advice.OnMethodEnter(inline = true, suppress = Throwable.class)
  public static Object enter(
      @Advice.Origin("#t") String type, @Advice.Origin("#m") String method, @Advice.Origin("#s") String signature) {
    return TraceDispatcher.INSTANCE.enter(type, method, signature, true);
  }

  @Advice.OnMethodExit(inline = true, onThrowable = Throwable.class, suppress = Throwable.class)
  public static void exit(@Advice.Enter Object node) {
    TraceDispatcher.INSTANCE.exit(node);
  }
}
