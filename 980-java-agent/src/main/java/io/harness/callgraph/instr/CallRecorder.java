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
package io.harness.callgraph.instr;

import io.harness.callgraph.CallGraph;
import io.harness.callgraph.util.StackNode;
import io.harness.callgraph.util.log.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CallRecorder {
  private static final Logger logger = new Logger(CallRecorder.class);
  /**
   * Collect the call graph per thread.
   */
  static final Map<Long, CallGraph> GRAPHS = new HashMap<>();
  public static void beforeMethod(StackNode node) {
    try {
      logger.trace(">> {}", node);
      Long threadId = Thread.currentThread().getId();
      if (!GRAPHS.containsKey(threadId)) {
        GRAPHS.put(threadId, new CallGraph(threadId));
      }
      GRAPHS.get(threadId).called(node);
    } catch (Throwable e) {
      logger.error("Error in beforeMethod", e);
    }
  }

  public static void afterMethod(StackNode node) {
    try {
      logger.trace("<< {}", node);
      long threadId = Thread.currentThread().getId();
      CallGraph graph = GRAPHS.get(threadId);
      if (graph == null) {
        // not interesting
        return;
      }
      graph.returned(node);
    } catch (Throwable e) {
      logger.error("Error in afterMethod", e);
    }
  }

  public static void shutdown() {
    for (CallGraph g : GRAPHS.values()) {
      try {
        g.finish();
      } catch (IOException e) {
        logger.error("Error finishing call graph {}", g, e);
      }
    }
  }
}
