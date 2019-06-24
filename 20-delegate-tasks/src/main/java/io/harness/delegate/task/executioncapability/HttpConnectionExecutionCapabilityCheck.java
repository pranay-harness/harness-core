package io.harness.delegate.task.executioncapability;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.network.Http;

public class HttpConnectionExecutionCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        (HttpConnectionExecutionCapability) delegateCapability;
    boolean valid = Http.connectableHttpUrl(httpConnectionExecutionCapability.fetchCapabilityBasis());
    return CapabilityResponse.builder().delegateCapability(httpConnectionExecutionCapability).validated(valid).build();
  }
}
