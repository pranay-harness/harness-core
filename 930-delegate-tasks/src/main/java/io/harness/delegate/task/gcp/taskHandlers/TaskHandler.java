package io.harness.delegate.task.gcp.taskHandlers;

import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.response.GcpResponse;

public interface TaskHandler {
  GcpResponse executeRequest(GcpRequest gcpRequest);
}
