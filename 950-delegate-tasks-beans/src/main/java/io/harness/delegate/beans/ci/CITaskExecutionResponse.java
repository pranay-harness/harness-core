package io.harness.delegate.beans.ci;

import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

public interface CITaskExecutionResponse extends DelegateTaskNotifyResponseData {
  enum Type { K8, VM }

  CITaskExecutionResponse.Type getType();
}
