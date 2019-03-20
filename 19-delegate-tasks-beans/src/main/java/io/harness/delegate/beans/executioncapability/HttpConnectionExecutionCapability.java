package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

@Value
@Builder
public class HttpConnectionExecutionCapability implements ExecutionCapability {
  private String scheme;
  private String hostName;
  private String port;
  private String url;
  @Default private final CapabilityType capabilityType = CapabilityType.HTTP;

  @Override
  public CapabilityType getCapabilityType() {
    return capabilityType;
  }

  @Override
  public String fetchCapabilityBasis() {
    // maintaining backward compatibility for now
    if (shouldUseOriginalUrl()) {
      return url;
    }

    StringBuilder builder = new StringBuilder(128);
    if (isNotBlank(scheme)) {
      builder.append(scheme).append("://");
    }

    builder.append(hostName);

    if (isNotBlank(port)) {
      builder.append(':').append(port);
    }

    return builder.toString();
  }

  private boolean shouldUseOriginalUrl() {
    return isBlank(scheme) && isBlank(hostName) && isBlank(port);
  }
}
