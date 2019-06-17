package io.harness.delegate.beans.executioncapability;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

/**
 * Checks that a TCP connection to a given (host, port) can be established.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class TcpConnectivityExecutionCapability extends NetCatExecutionCapability {
  public TcpConnectivityExecutionCapability(String hostName, int port) {
    super(hostName, "", String.valueOf(port), "");
  }

  @Override
  public List<String> processExecutorArguments() {
    return Arrays.asList("nc", "-z", "-G5", hostName, port);
  }
}
