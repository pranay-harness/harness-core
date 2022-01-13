/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.LiteEngineConnectionCapability;
import io.harness.product.ci.engine.proto.LiteEngineGrpc;
import io.harness.product.ci.engine.proto.PingRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.GrpcUtil;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LiteEngineConnectionCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    LiteEngineConnectionCapability liteEngineConnectionCapability = (LiteEngineConnectionCapability) delegateCapability;
    boolean valid = isConnectibleLiteEngine(liteEngineConnectionCapability.getIp(),
        liteEngineConnectionCapability.getPort(), liteEngineConnectionCapability.isLocal());
    return CapabilityResponse.builder().delegateCapability(liteEngineConnectionCapability).validated(valid).build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.LITE_ENGINE_CONNECTION_PARAMETERS) {
      return builder.permissionResult(CapabilitySubjectPermission.PermissionResult.DENIED).build();
    }

    return builder
        .permissionResult(isConnectibleLiteEngine(parameters.getLiteEngineConnectionParameters().getIp(),
                              parameters.getLiteEngineConnectionParameters().getPort(),
                              parameters.getLiteEngineConnectionParameters().getIsLocal())
                ? CapabilitySubjectPermission.PermissionResult.ALLOWED
                : CapabilitySubjectPermission.PermissionResult.DENIED)
        .build();
  }

  private boolean isConnectibleLiteEngine(String ip, int port, boolean isLocal) {
    String target = String.format("%s:%d", ip, port);

    ManagedChannelBuilder managedChannelBuilder = ManagedChannelBuilder.forTarget(target).usePlaintext();
    if (!isLocal) {
      managedChannelBuilder.proxyDetector(GrpcUtil.NOOP_PROXY_DETECTOR);
    }
    ManagedChannel channel = managedChannelBuilder.build();
    try {
      try {
        LiteEngineGrpc.LiteEngineBlockingStub liteEngineBlockingStub = LiteEngineGrpc.newBlockingStub(channel);
        liteEngineBlockingStub.withDeadlineAfter(120, TimeUnit.SECONDS).ping(PingRequest.newBuilder().build());
        return true;
      } finally {
        // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
        // resources the channel should be shut down when it will no longer be used. If it may be used
        // again leave it running.
        channel.shutdownNow();
      }
    } catch (Exception e) {
      log.error("Failed to connect to lite engine target {} with err: {}", target, e);
    }
    return false;
  }
}
