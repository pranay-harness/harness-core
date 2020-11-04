package graph;

import io.harness.yaml.core.intfc.WithIdentifier;

import java.util.List;
import java.util.Set;

/**
 *    Graph to store steps dependency and ordering
 */

public interface Graph<T extends WithIdentifier> {
  List<String> getNextNodeUuids(T currentNode);

  String getStartNodeUuid();

  void addNode(T node);

  void addEdge(T from, T to);

  Set<String> getEdges(String nodeId);

  T getNode(String nodeId);

  List<T> getAllNodes();
}