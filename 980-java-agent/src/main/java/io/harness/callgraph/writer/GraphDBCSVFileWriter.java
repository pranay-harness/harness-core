package io.harness.callgraph.writer;

import io.harness.callgraph.util.StackNode;

import java.io.IOException;

public class GraphDBCSVFileWriter implements GraphWriter {
  private FileWriter writer;

  public GraphDBCSVFileWriter(long threadId) throws IOException {
    if (writer == null) {
      writer = new FileWriter(String.format("/cg/graphdb-%d.csv", threadId));
    }
  }

  @Override
  public void node(StackNode method) {}

  @Override
  public void edge(StackNode from, StackNode to) throws IOException {
    StringBuilder line = new StringBuilder();
    String fromString = from.toGraphDBCSV();
    String toString = to.toGraphDBCSV();

    line.append(fromString.hashCode()).append('|');
    line.append(fromString).append('|');
    line.append(toString.hashCode()).append('|');
    line.append(toString).append('\n');
    // There should be a single call to writer.append(), otherwise multiple threads will jumble up the data
    writer.append(line.toString());
  }

  @Override
  public void end() {}

  @Override
  public void close() throws IOException {
    if (writer == null) {
      // TODO: log error
    } else {
      writer.close();
    }
  }
}
