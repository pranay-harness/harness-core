package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * Level is a combination for the setupId and runtime executionId for a particular entity which runs
 * Examples:
 *
 * Node is a level : nodeId, nodeExecutionInstanceId
 */
@OwnedBy(CDC)
@Redesign
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "LevelKeys")
@EqualsAndHashCode
public class Level {
  @Getter String setupId;
  @Getter String runtimeId;
  @Getter String identifier;
  @Getter StepType stepType;

  @Builder
  public Level(String setupId, String runtimeId, String identifier, StepType stepType) {
    this.setupId = setupId;
    this.runtimeId = runtimeId;
    this.identifier = identifier;
    this.stepType = stepType;
  }
}
