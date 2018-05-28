package software.wings.beans;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class GraphGroup.
 */
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class GraphGroup extends GraphNode {
  private List<GraphNode> elements = new ArrayList<>();

  private ExecutionStrategy executionStrategy = ExecutionStrategy.PARALLEL;

  /**
   * Instantiates a new Group.
   */
  public GraphGroup() {
    setType("GROUP");
  }

  /**
   * Gets elements.
   *
   * @return the elements
   */
  public List<GraphNode> getElements() {
    return elements;
  }

  /**
   * Sets elements.
   *
   * @param elements the elements
   */
  public void setElements(List<GraphNode> elements) {
    this.elements = elements;
  }

  /**
   * Gets execution strategy.
   *
   * @return the execution strategy
   */
  public ExecutionStrategy getExecutionStrategy() {
    return executionStrategy;
  }

  /**
   * Sets execution strategy.
   *
   * @param executionStrategy the execution strategy
   */
  public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
    this.executionStrategy = executionStrategy;
  }
}
