/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.citasks;

import io.harness.delegate.beans.ci.ExecuteCommandTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;

public interface ExecuteCommandTaskHandler {
  enum Type { K8 }
  ExecuteCommandTaskHandler.Type getType();
  K8sTaskExecutionResponse executeTaskInternal(ExecuteCommandTaskParams executeCommandTaskParams);
}
