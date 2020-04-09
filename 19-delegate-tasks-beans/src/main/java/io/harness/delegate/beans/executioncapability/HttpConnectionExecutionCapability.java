package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpConnectionExecutionCapability implements ExecutionCapability {
  private final CapabilityType capabilityType = CapabilityType.HTTP;

  private String url;

  private String host;
  private String scheme;
  private int port;
  private String path;
  private String query;

  @Override
  public String fetchCapabilityBasis() {
    if (url != null) {
      return url;
    }

    StringBuilder builder = new StringBuilder(128);
    if (isNotBlank(scheme)) {
      builder.append(scheme).append("://");
    }
    builder.append(host);
    if (port != -1) {
      builder.append(':').append(port);
    }
    if (isNotBlank(path)) {
      builder.append('/').append(path);
    }
    if (isNotBlank(query)) {
      builder.append('?').append(query);
    }
    return builder.toString();
  }
}
