package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.WinRMExecutionCapability;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WinRMExecutionCapabilityGenerator {
  public static WinRMExecutionCapability buildWinRMExecutionCapability(@NonNull String hostName, @NonNull String port) {
    return WinRMExecutionCapability.builder().hostName(hostName).port(port).url(hostName + ":" + port).build();
  }
}
