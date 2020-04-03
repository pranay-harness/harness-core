package io.harness.plan;

import io.harness.annotations.Redesign;
import io.harness.persistence.PersistentEntity;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
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
@FieldNameConstants(innerTypeName = "ExecutionPlanKeys")
public class ExecutionPlan implements ExecutionNode, PersistentEntity {
  @Id @NotNull(groups = {Update.class}) String uuid;
  @Singular Map<String, ExecutionNode> nodes;
  @NotNull String originId;

  public static ExecutionPlan emptyPlan() {
    return builder().build();
  }

  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  public ExecutionNodeDefinition getInitialNode() {
    if (isEmpty()) {
      return null;
    }
    return (ExecutionNodeDefinition) nodes.get(originId);
  }
}
