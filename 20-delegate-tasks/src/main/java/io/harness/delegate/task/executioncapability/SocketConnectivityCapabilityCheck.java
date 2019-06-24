package io.harness.delegate.task.executioncapability;

import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
@Singleton
public class SocketConnectivityCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SocketConnectivityExecutionCapability socketConnCapability =
        (SocketConnectivityExecutionCapability) delegateCapability;
    boolean valid;
    if (socketConnCapability.getHostName() != null) {
      valid = connectableHost(socketConnCapability.getHostName(), Integer.valueOf(socketConnCapability.getPort()));
    } else {
      valid = connectableHost(socketConnCapability.getUrl(), Integer.valueOf(socketConnCapability.getPort()));
    }
    return CapabilityResponse.builder().delegateCapability(socketConnCapability).validated(valid).build();
  }

  public static boolean connectableHost(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 5000); // 5 sec timeout
      return true;
    } catch (IOException ignored) {
      // Do nothing
    }
    return false; // Either timeout or unreachable or failed DNS lookup.
  }
}
