package io.harness.delegate.beans.executioncapability;

import static io.harness.govern.Switch.unhandled;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class HttpConnectionExecutionCapability extends NetCatExecutionCapability {
  @Builder
  private HttpConnectionExecutionCapability(String hostName, String scheme, String port, @NonNull String url) {
    super(hostName, scheme, port, url);
  }

  @Override
  public List<String> processExecutorArguments() {
    return Arrays.asList("nc", "-z", "-G5", isBlank(hostName) ? url : hostName, getDefaultPort());
  }

  private String getDefaultPort() {
    if (!isBlank(port)) {
      return port;
    } else if (isNotBlank(scheme)) {
      switch (scheme.toUpperCase()) {
        case "HTTP":
          return "80";
        case "HTTPS":
          return "443";
        default:
          unhandled(scheme);
          return "80";
      }
    } else {
      return "80";
    }
  }
}
