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
 * This is the Graph of state which is the part of the execution plan
 * This consist a list of GraphNodes which can be either a leaf node i.e GraphNodeDefinition
 * or it can be a StateGraph itself
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
