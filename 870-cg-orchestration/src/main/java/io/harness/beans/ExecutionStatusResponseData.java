/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import io.harness.delegate.beans.DelegateResponseData;

/**
 * The type Execution status data.
 */
public interface ExecutionStatusResponseData extends DelegateResponseData {
  ExecutionStatus getExecutionStatus();
}
