package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.NonNull;

import java.util.List;

public abstract class NetCatExecutionCapability implements ExecutionCapability {
  protected String hostName;
  protected String scheme;
  protected String port;
  @NonNull protected String url;
  private final CapabilityType capabilityType = CapabilityType.NETCAT;

  public NetCatExecutionCapability(String hostName, String scheme, String port, @NonNull String url) {
    this.hostName = hostName;
    this.scheme = scheme;
    this.port = port;
    this.url = url;
  }

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

  public abstract List<String> processExecutorArguments();
}
