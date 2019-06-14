package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Builder;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;

public class WinRMExecutionCapability extends NetCatExecutionCapability {
  @Builder
  private WinRMExecutionCapability(String hostName, String scheme, String port, @NonNull String url) {
    super(hostName, scheme, port, url);
  }

  @Override
  public List<String> processExecutorArguments() {
    return Arrays.asList("nc", "-z", "-G5", isBlank(hostName) ? url : hostName, isBlank(port) ? "5985" : port);
  }
}
