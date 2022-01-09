/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.citasks;

import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.CITaskExecutionResponse;

public interface CIExecuteStepTaskHandler {
  enum Type { K8, VM }

  CIExecuteStepTaskHandler.Type getType();

  CITaskExecutionResponse executeTaskInternal(CIExecuteStepTaskParams ciExecuteStepTaskParams, String taskId);
}
