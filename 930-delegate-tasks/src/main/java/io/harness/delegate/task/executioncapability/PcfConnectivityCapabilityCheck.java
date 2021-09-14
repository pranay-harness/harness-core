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
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.PcfConnectivityCapability;
import io.harness.network.Http;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PcfConnectivityCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    PcfConnectivityCapability pcfConnectivityCapability = (PcfConnectivityCapability) delegateCapability;
    try {
      boolean httpEndpointConnectable = isEndpointConnectable(pcfConnectivityCapability.getEndpointUrl(), "http://");
      boolean httpsEndpointConnectable = isEndpointConnectable(pcfConnectivityCapability.getEndpointUrl(), "https://");

      return CapabilityResponse.builder()
          .delegateCapability(pcfConnectivityCapability)
          .validated(httpEndpointConnectable || httpsEndpointConnectable)
          .build();
    } catch (Exception e) {
      log.error("Failed to connect, RepoUrl: {}", pcfConnectivityCapability.getEndpointUrl());
      return CapabilityResponse.builder().delegateCapability(pcfConnectivityCapability).validated(false).build();
    }
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.PCF_CONNECTIVITY_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }

    String endpointUrl = parameters.getPcfConnectivityParameters().getEndpointUrl();
    try {
      boolean httpEndpointConnectable = isEndpointConnectable(endpointUrl, "http://");
      boolean httpsEndpointConnectable = isEndpointConnectable(endpointUrl, "https://");

      return builder
          .permissionResult((httpEndpointConnectable || httpsEndpointConnectable) ? PermissionResult.ALLOWED
                                                                                  : PermissionResult.DENIED)
          .build();
    } catch (Exception e) {
      log.error("Failed to connect, RepoUrl: {}", endpointUrl);
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
  }

  boolean isEndpointConnectable(String endpointUrl, String urlScheme) {
    return Http.connectableHttpUrl(urlScheme + endpointUrl);
  }
}
