package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;

import javax.validation.constraints.NotNull;

public class SocketConnectivityCapabilityGenerator {
  public static SocketConnectivityExecutionCapability buildSocketConnectivityCapability(
      @NotNull String hostName, @NotNull String port) {
    return SocketConnectivityExecutionCapability.builder()
        .hostName(hostName)
        .port(port)
        .url(hostName + ":" + port)
        .build();
  }
}
