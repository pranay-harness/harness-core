/*
 * MIT License
 * <p>
 * Copyright (c) 2017 David Krebs
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package harness.callgraph.instr.tracer;

import harness.callgraph.boot.TraceDispatcher;
import net.bytebuddy.asm.Advice;

public class ConstructorTracer {
  @Advice.OnMethodEnter(inline = true, suppress = Throwable.class)
  public static Object enter(
      @Advice.Origin("#t") String type, @Advice.Origin("#m") String method, @Advice.Origin("#s") String signature) {
    return TraceDispatcher.INSTANCE.enter(type, method, signature, false);
  }

  @Advice.OnMethodExit(inline = true, suppress = Throwable.class)
  public static void exit(@Advice.Enter Object node) {
    TraceDispatcher.INSTANCE.exit(node);
  }
}
