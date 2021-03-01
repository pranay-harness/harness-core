package io.harness.callgraph.writer;

import io.harness.callgraph.util.StackNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.json.JSONObject;

class Relation {
  StackNode source, test;
  Relation(StackNode source, StackNode test) {
    this.source = source;
    this.test = test;
  }
}

public class JSONCoverageFileWriter implements GraphWriter {
  FileWriter writer;
  final int SET_CAPACITY = 10000;
  private Set<Relation> relations = new HashSet<>(SET_CAPACITY);
  private Set<Integer> relationsHash = new HashSet<>(SET_CAPACITY);

  private StackNode currentItem;

  public JSONCoverageFileWriter(long threadId) throws IOException {
    if (writer == null) {
      writer = new FileWriter(String.format("/cg/coverage-%d.json", threadId));
    }
  }

  @Override
  public void node(StackNode method) throws IOException {
    currentItem = method.isTestMethod() ? method : null;
  }

  @Override
  public void edge(StackNode from, StackNode to) throws IOException {
    if (currentItem == null && from.isTestMethod()) {
      currentItem = from;
    }
    int hash = Objects.hash(to, currentItem);
    if (relationsHash.contains(hash)) {
      // duplicate relation, ignore
      return;
    }
    relationsHash.add(hash);
    relations.add(new Relation(to, currentItem));
    if (relations.size() == SET_CAPACITY) {
      flushRelations();
    }
  }

  // Write relations to file
  private void flushRelations() throws IOException {
    for (Relation relation : relations) {
      writeToFile(relation.source, relation.test);
    }
    relations.clear();
    relationsHash.clear();
  }

  private void writeToFile(StackNode source, StackNode test) throws IOException {
    if (source == null || test == null) {
      return;
    }
    JSONObject json = new JSONObject();
    Map<String, Object> node = getStringObjectMap(source);
    json.put("source", node);

    node = getStringObjectMap(test);
    json.put("test", node);
    writer.append(json.toString() + '\n');
  }

  @Override
  public void end() throws IOException {
    flushRelations();
  }

  @Override
  public void close() throws IOException {
    flushRelations();
    writer.close();
  }

  private Map<String, Object> getStringObjectMap(StackNode key) {
    String parameters = key.getSignature();
    if (parameters.isEmpty()) {
      parameters = "void";
    }
    Map<String, Object> source = new HashMap<>();
    source.put("package", key.getPackageName());
    source.put("class", key.getClassName());
    source.put("method", key.getMethodName());
    source.put("params", parameters);
    int hashCode = source.hashCode();
    source.put("id", hashCode);
    return source;
  }
}
