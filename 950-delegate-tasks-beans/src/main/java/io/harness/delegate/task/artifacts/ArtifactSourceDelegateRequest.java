/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts;

import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;

/**
 * Interface of DTO to be passed to Delegate Tasks.
 */
public interface ArtifactSourceDelegateRequest extends ExecutionCapabilityDemander {
  ArtifactSourceType getSourceType();
}
