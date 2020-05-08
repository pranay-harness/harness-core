package graph;

/**
 *    Graph to store steps dependency and ordering
 */

public interface Graph<T> {
  String getNextNodeUuid(T currentNode);

  String getStartNodeUuid();
}
