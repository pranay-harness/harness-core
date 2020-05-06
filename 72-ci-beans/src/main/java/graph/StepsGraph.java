package graph;

/**
 *    Graph to store steps dependency and ordering
 */

public interface StepsGraph<T> {
  String getNextStepUuid(T ciStep);

  String getStartNodeUuid();
}
