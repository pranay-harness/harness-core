package io.harness.delegate.beans.executioncapability;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class SocketConnectivityExecutionCapability extends TcpBasedExecutionCapability {
  private final CapabilityType capabilityType = CapabilityType.SOCKET;

  @Override
  public CapabilityType getCapabilityType() {
    return capabilityType;
  }

  @Builder
  public SocketConnectivityExecutionCapability(String hostName, String scheme, String port, String url) {
    super(hostName, scheme, isNotBlank(port) ? port : "22", url);
  }
}
