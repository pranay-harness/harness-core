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

  // formatting for GraphDB CSV
  private String toGraphDBCSV(StackNode node) {
    StringBuffer result = new StringBuffer();

    String parameters = node.getSignature();
    //    if (isEmpty(parameters)) { //TODO
    if (parameters.isEmpty()) {
      parameters = "void";
    }

    String codeType = node.isTestMethod() ? "Test" : "Source";
    result.append(codeType);
    result.append("|").append(node.getPackageName());
    result.append("|").append(node.getClassName());
    result.append("|").append(node.getMethodName());
    result.append("|").append(parameters);

    return result.toString();
  }
  @Override
  public void node(StackNode method) {}

  @Override
  public void edge(StackNode from, StackNode to) throws IOException {
    StringBuilder line = new StringBuilder();
    String fromString = toGraphDBCSV(from);
    String toString = toGraphDBCSV(to);

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
