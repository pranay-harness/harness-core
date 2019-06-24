package io.harness.delegate.beans.executioncapability;

import static java.util.Collections.singletonList;

import lombok.Value;

import java.util.List;

@Value
public class ConnectivityCapabilityDemander implements ExecutionCapabilityDemander {
  String host;
  int port;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return singletonList(
        SocketConnectivityExecutionCapability.builder().hostName(host).port(String.valueOf(port)).build());
  }
}
