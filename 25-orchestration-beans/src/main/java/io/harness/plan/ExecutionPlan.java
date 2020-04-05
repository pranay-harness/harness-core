package io.harness.plan;

import io.harness.annotations.Redesign;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * This is the Graph of state which is the part of the execution plan
 * This consist a list of GraphNodes which can be either a leaf node i.e GraphNodeDefinition
 * or it can be a StateGraph itself
 */

@Value
@Builder
@Redesign
@Entity(value = "executionPlan", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "ExecutionPlanKeys")
public class ExecutionPlan implements PersistentEntity {
  @Id @NotNull String uuid;

  @Singular Map<String, ExecutionNode> nodes;

  @NotNull String startingNodeId;

  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  public ExecutionNode fetchStartingNode() {
    if (isEmpty()) {
      return null;
    }
    return nodes.get(startingNodeId);
  }
}
