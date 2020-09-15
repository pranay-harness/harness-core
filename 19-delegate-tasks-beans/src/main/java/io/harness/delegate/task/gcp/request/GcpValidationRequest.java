package io.harness.delegate.task.gcp.request;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class GcpValidationRequest extends GcpRequest implements TaskParameters {
  @Builder
  public GcpValidationRequest(String delegateSelector) {
    super(delegateSelector, RequestType.VALIDATE);
  }
}
