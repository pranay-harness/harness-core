/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;

/**
 * The Class GraphGroup.
 */
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._957_CG_BEANS)
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
