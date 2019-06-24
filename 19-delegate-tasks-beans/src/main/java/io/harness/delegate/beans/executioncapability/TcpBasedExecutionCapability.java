package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public abstract class TcpBasedExecutionCapability implements ExecutionCapability {
  protected String hostName;
  protected String scheme;
  protected String port;
  @NonNull protected String url;

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
    return isBlank(scheme) && isBlank(hostName);
  }
}
