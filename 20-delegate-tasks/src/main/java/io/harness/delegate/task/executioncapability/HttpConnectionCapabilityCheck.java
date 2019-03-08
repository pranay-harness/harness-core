package io.harness.delegate.task.executioncapability;

import static io.harness.network.Http.connectableHttpUrl;

import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.CapabilityCheckResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;

@Singleton

public class HttpConnectionCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityCheckResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        (HttpConnectionExecutionCapability) delegateCapability;

    return CapabilityCheckResponse.builder()
        .delegateCapability(httpConnectionExecutionCapability)
        .validated(connectableHttpUrl(httpConnectionExecutionCapability.fetchCapabilityBasis()))
        .build();
  }
}
