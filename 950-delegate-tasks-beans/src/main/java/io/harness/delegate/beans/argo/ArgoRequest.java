package io.harness.delegate.beans.argo;

import io.harness.delegate.task.TaskParameters;

public interface ArgoRequest extends TaskParameters {
  String getAppName();
  RequestType requestType();
}
