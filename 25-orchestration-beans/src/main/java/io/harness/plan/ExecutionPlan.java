package io.harness.plan;

import io.harness.annotations.Redesign;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * This is the plan we want to execute during the execution
 * It contains a list of ExecutionNode and stating point
 * This is contained as a list sorted by the uuid to quick retrieval.
 *
 * Do not want this to be a map as we will lost the ability to query the database by node properties
 * This will be required by the apps to performs some migrations etc.
 *
 * This was a major pain point for the design of our StateMachine.
 *
 * With this approach we can crate iterators over these and perform the migrations
 */

@Value
@Builder(buildMethodName = "internalBuild")
@Redesign
@Entity(value = "executionPlan", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "ExecutionPlanKeys")
public class ExecutionPlan implements PersistentEntity {
  @Id @NotNull String uuid;

  @Singular List<ExecutionNode> nodes;

  @NotNull String startingNodeId;

  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  public ExecutionNode fetchStartingNode() {
    return fetchNode(startingNodeId);
  }

  public ExecutionNode fetchNode(String nodeId) {
    return nodes.get(Collections.binarySearch(
        nodes, ExecutionNode.builder().uuid(nodeId).build(), Comparator.comparing(ExecutionNode::getUuid)));
  }

  public static class ExecutionPlanBuilder {
    public ExecutionPlan build() {
      nodes.sort(Comparator.comparing(ExecutionNode::getUuid));
      return internalBuild();
    }
  }
}
