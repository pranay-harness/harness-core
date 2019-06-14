package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class SSHConnectionExecutionCapability extends NetCatExecutionCapability {
  @Builder
  private SSHConnectionExecutionCapability(String hostName, String scheme, String port, @NonNull String url) {
    super(hostName, scheme, port, url);
  }

  public List<String> processExecutorArguments() {
    return Arrays.asList("nc", "-z", "-G5", isBlank(hostName) ? url : hostName, isBlank(port) ? "22" : port);
  }
}
