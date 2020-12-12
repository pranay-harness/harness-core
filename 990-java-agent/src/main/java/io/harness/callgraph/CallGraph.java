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
package io.harness.callgraph;

import io.harness.callgraph.util.StackNode;
import io.harness.callgraph.util.Target;
import io.harness.callgraph.util.config.Config;
import io.harness.callgraph.writer.GraphDBCSVFileWriter;
import io.harness.callgraph.writer.GraphWriter;
import io.harness.callgraph.writer.JSONCoverageFileWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class CallGraph {
  private final long threadId;
  final Stack<StackNode> calls = new Stack<>();

  final List<GraphWriter> writers = new ArrayList<>();

  public CallGraph(long threadId) throws IOException {
    this.threadId = threadId;
    Target[] targets;
    targets = Config.getInst() == null ? new Target[] {Target.COVERAGE_JSON} : Config.getInst().writeTo();
    for (Target target : targets) {
      writers.add(createWriter(target));
    }
  }

  public GraphWriter createWriter(Target t) throws IOException {
    switch (t) {
      case COVERAGE_JSON:
        return new JSONCoverageFileWriter(this.threadId);
      case GRAPH_DB_CSV:
        return new GraphDBCSVFileWriter(this.threadId);
      default:
        throw new IllegalArgumentException("Unknown writeTo: " + t);
    }
  }

  public void called(StackNode method) throws IOException {
    if (calls.isEmpty() && method.isTestMethod()) { // first item pushed to stack should be test

      // First node
      calls.push(method);
      for (GraphWriter w : writers) {
        w.node(method);
      }

    } else if (!calls.isEmpty()) { // it's not a first node in the stack
      StackNode top = calls.peek();

      for (GraphWriter w : writers) {
        w.edge(top, method);
      }
      calls.push(method);
    } else {
      // The first call is not from test method, so ignore it
    }
  }

  public void returned(StackNode method) throws IOException {
    while (!calls.isEmpty()) {
      StackNode topNode = calls.pop();
      if (topNode.equals(method)) {
        break;
      }
    }

    if (calls.isEmpty()) {
      for (GraphWriter w : writers) {
        w.end();
      }
    }
  }

  public void finish() throws IOException {
    for (GraphWriter w : writers) {
      w.close();
    }
  }
}
