package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class HttpConnectionExecutionCapability extends TcpBasedExecutionCapability {
  private final CapabilityType capabilityType = CapabilityType.HTTP;

  @Builder
  public HttpConnectionExecutionCapability(String hostName, String scheme, String port, @NonNull String url) {
    super(hostName, scheme,
        isNotBlank(port) ? port : (isNotBlank(scheme) && scheme.equalsIgnoreCase("HTTPS")) ? "443" : "80", url);
  }
}
