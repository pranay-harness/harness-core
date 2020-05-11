package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

/**
 * Level is a combination for the setupId and runtime executionId for a particular entity which runs
 * Examples:
 *
 * Node is a level : nodeId, nodeExecutionInstanceId
 */
@OwnedBy(CDC)
@Value
@Builder
@Redesign
@FieldNameConstants(innerTypeName = "LevelExecutionKeys")
public class LevelExecution {
  String setupId;
  String runtimeId;
  @NonNull Level level;

  public LevelType getLevelName() {
    return level.getType();
  }

  public int getLevelPriority() {
    return level.getOrder();
  }
}
