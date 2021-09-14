/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.taskprogress;

import io.harness.delegate.beans.DelegateProgressData;

public interface ITaskProgressClient {
  /**
   *
   * Sends an update about the task progress to the manager.
   */
  boolean sendTaskProgressUpdate(DelegateProgressData delegateProgressData);
}
