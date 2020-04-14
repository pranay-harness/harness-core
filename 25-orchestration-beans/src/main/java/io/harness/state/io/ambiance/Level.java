package io.harness.state.io.ambiance;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

/**
 * Level is a combination for the setupId and runtime executionId for a particular entity which runs
 * Examples:
 *
 * Node is a level : nodeId, nodeExecutionInstanceId
 */
@Value
@Builder
@Redesign
public class Level {
  String setupId;
  String runtimeId;
}
