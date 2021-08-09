package io.harness.delegate.beans.taskprogress;

import io.harness.delegate.beans.DelegateProgressData;

public interface ITaskProgressClient {
  /**
   *
   * Sends an update about the task progress to the manager.
   */
  boolean sendTaskProgressUpdate(DelegateProgressData delegateProgressData);
}
