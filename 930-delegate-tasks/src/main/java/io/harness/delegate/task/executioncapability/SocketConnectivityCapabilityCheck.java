package io.harness.delegate.task.executioncapability;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.expression.ExpressionEvaluator;

import com.google.inject.Singleton;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SocketConnectivityCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SocketConnectivityExecutionCapability socketConnCapability =
        (SocketConnectivityExecutionCapability) delegateCapability;
    try {
      boolean valid;
      if (socketConnCapability.getHostName() != null) {
        valid = connectableHost(socketConnCapability.getHostName(), Integer.parseInt(socketConnCapability.getPort()));
      } else {
        valid = connectableHost(socketConnCapability.getUrl(), Integer.parseInt(socketConnCapability.getPort()));
      }
      return CapabilityResponse.builder().delegateCapability(socketConnCapability).validated(valid).build();
    } catch (Exception ex) {
      log.error("Error Occurred while checking socketConnCapability: {}", ex.getMessage());
      return CapabilityResponse.builder().delegateCapability(socketConnCapability).validated(false).build();
    }
  }

  public static boolean connectableHost(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 5000); // 5 sec timeout
      log.info("[Delegate Capability] Socket Connection Succeeded for url " + host + "on port" + port);
      return true;
    } catch (IOException ignored) {
      log.error("[Delegate Capability] Socket Connection Failed for url " + host + "on port" + port);
    }
    return false; // Either timeout or unreachable or failed DNS lookup.
  }
}
