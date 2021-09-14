/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.SocketConnectivityParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;

import com.google.inject.Singleton;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SocketConnectivityCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
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

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.SOCKET_CONNECTIVITY_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    SocketConnectivityParameters socketParameters = parameters.getSocketConnectivityParameters();
    try {
      return builder
          .permissionResult(connectableHost(socketParameters.getHostName().isEmpty() ? socketParameters.getUrl()
                                                                                     : socketParameters.getHostName(),
                                socketParameters.getPort())
                  ? PermissionResult.ALLOWED
                  : PermissionResult.DENIED)
          .build();
    } catch (Exception ex) {
      log.error("Error Occurred while checking socketConnCapability: {}", ex.getMessage());
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
  }

  public static boolean connectableHost(String host, int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 5000); // 5 sec timeout
      log.info("[Delegate Capability] Socket Connection Succeeded for url " + host + " on port " + port);
      return true;
    } catch (IOException ignored) {
      log.error("[Delegate Capability] Socket Connection Failed for url " + host + " on port " + port);
    }
    return false; // Either timeout or unreachable or failed DNS lookup.
  }
}
