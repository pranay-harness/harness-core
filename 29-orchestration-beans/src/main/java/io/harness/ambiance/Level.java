package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plan.PlanNode;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import javax.validation.constraints.NotNull;

/**
 * Level is a combination for the setupId and runtime executionId for a particular entity which runs
 * Examples:
 *
 * Node is a level : nodeId, nodeExecutionInstanceId
 */
@OwnedBy(CDC)
@Redesign
@Value
@Builder
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "LevelKeys")
public class Level {
  String setupId;
  String runtimeId;
  String identifier;
  StepType stepType;
  String group;

  public static Level fromPlanNode(@NotNull String nodeExecutionId, @NotNull PlanNode node) {
    return Level.builder()
        .setupId(node.getUuid())
        .runtimeId(nodeExecutionId)
        .identifier(node.getIdentifier())
        .stepType(node.getStepType())
        .group(node.getGroup())
        .build();
  }
}
