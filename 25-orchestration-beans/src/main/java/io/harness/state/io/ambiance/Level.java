package io.harness.state.io.ambiance;

import lombok.Builder;
import lombok.Value;

/**
 * Level is a combination for the setupId and runtime executionId for a particular entity which runs
 * Examples:
 *
 * Node is a level : nodeId, nodeExecutionInstanceId
 * Workflow is a level: workflowId, workflowExecutionId
 */
@Value
@Builder
public class Level {
  String entityId;
  String executionId;
}
