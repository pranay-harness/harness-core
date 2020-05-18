package io.harness.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

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
@FieldNameConstants(innerTypeName = "LevelKeys")
public class Level {
  String setupId;
  String runtimeId;
  String identifier;
}
