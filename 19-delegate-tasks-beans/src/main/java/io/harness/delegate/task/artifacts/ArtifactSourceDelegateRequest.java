package io.harness.delegate.task.artifacts;

import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;

/**
 * Interface of DTO to be passed to Delegate Tasks.
 */
public interface ArtifactSourceDelegateRequest extends ExecutionCapabilityDemander {
  ArtifactSourceType getSourceType();
}
