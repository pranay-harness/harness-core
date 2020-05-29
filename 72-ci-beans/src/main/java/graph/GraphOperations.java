package graph;

import io.harness.yaml.core.intfc.WithIdentifier;

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GraphOperations is a collection of common graph traversals and operations.
 *  */
public class GraphOperations<T extends WithIdentifier> {
  /**
   * Finds all leafs in graph. Leaf is node that doesn't have any edge coming out of it.
   * @param graph input graph
   * @return set of leafs
   */
  public Set<T> findLeafs(Graph<T> graph) {
    return graph.getAllNodes()
        .stream()
        // find each node that doesn't have any edge coming out of it
        .filter(node -> graph.getEdges(node.getIdentifier()) == null || graph.getEdges(node.getIdentifier()).isEmpty())
        .collect(Collectors.toSet());
  }

  /**
   * Finds all roots in a graph. Each root is a node that doesn't have any edge coming into it.
   * @param graph input graph
   * @return set of roots
   */
  public Set<T> findRoots(Graph<T> graph) {
    // set of all nodes ids
    final Set<String> allNodeIds = graph.getAllNodes().stream().map(T::getIdentifier).collect(Collectors.toSet());

    // set of all nodes that are children
    final Set<String> allEdges = graph.getAllNodes()
                                     .stream()
                                     .map(node -> graph.getEdges(node.getIdentifier()))
                                     .flatMap(Set::stream)
                                     .collect(Collectors.toSet());

    // relative complement of allNodes to allEdges are roots
    return allNodeIds.stream().filter(node -> !allEdges.contains(node)).map(graph::getNode).collect(Collectors.toSet());
  }

  /**
   * Sorts the graph nodes in topological order. Topological order is a linear ordering of its nodes such that for every
   * directed edge uv from node u to node v, u comes before v in the ordering.
   * @param nodeGraph input graph to sort
   * @return list of nodes in topological order
   */
  public List<T> topologicalSort(Graph<T> nodeGraph) {
    LinkedList<T> stack = new LinkedList<>();
    Set<String> nodeVisited = new HashSet<>();
    // iterate through all the nodes and their neighbours if not already visited.
    for (T step : nodeGraph.getAllNodes()) {
      if (!nodeVisited.contains(step.getIdentifier())) {
        topologicalSortUtil(step, stack, nodeVisited, nodeGraph);
      }
    }
    return stack;
  }

  // This recursive method iterates through all the nodes and neighbours.
  // Pushes the visited items to stack
  private void topologicalSortUtil(T node, Deque<T> stack, Set<String> nodeVisited, Graph<T> nodeGraph) {
    // add the visited node to list, so we don't repeat this node again
    nodeVisited.add(node.getIdentifier());
    // the leaf nodes wouldn't have neighbors. A check added to avoid null pointer
    if (nodeGraph.getEdges(node.getIdentifier()) != null) {
      // get all the neighbor nodes , by referring its edges
      Iterator<String> iter = nodeGraph.getEdges(node.getIdentifier()).iterator();
      String neighborNodeId;
      // if an edge exists for the node, then visit that neighbor node
      while (iter.hasNext()) {
        neighborNodeId = iter.next();
        if (!nodeVisited.contains(neighborNodeId)) {
          topologicalSortUtil(nodeGraph.getNode(neighborNodeId), stack, nodeVisited, nodeGraph);
        }
      }
    }
    // push the latest node on to the stack
    stack.push(node);
  }
}
