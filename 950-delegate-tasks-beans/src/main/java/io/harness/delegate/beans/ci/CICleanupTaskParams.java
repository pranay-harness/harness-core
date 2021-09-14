/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci;

import io.harness.delegate.task.TaskParameters;

public interface CICleanupTaskParams extends TaskParameters {
  enum Type { GCP_K8 }

  CICleanupTaskParams.Type getType();
}
