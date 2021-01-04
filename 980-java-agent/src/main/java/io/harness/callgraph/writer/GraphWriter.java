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
package io.harness.callgraph.writer;

import io.harness.callgraph.util.StackNode;

import java.io.Closeable;
import java.io.IOException;

public interface GraphWriter extends Closeable {
  /**
   * Add a node.This is only called once at the beginning of the graph.
   *
   * @param method method
   */
  void node(StackNode method) throws IOException;

  /**
   * Add an edge from one method to the other.
   *
   * @param from source node
   * @param to   target node
   */
  void edge(StackNode from, StackNode to) throws IOException;

  /**
   * Finish the graph.
   */
  void end() throws IOException;

  /**
   * Finally called after all graphs are written.
   */
  @Override void close() throws IOException;
}
