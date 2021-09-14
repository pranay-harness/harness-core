/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import io.harness.beans.KeyValuePair;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionBuilder;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.HttpConnectionParameters;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.network.Http;

import java.util.stream.Collectors;

public class HttpConnectionExecutionCapabilityCheck implements CapabilityCheck, ProtoCapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        (HttpConnectionExecutionCapability) delegateCapability;
    boolean valid;
    if (httpConnectionExecutionCapability.getHeaders() != null) {
      valid = Http.connectableHttpUrlWithHeaders(
          httpConnectionExecutionCapability.fetchConnectableUrl(), httpConnectionExecutionCapability.getHeaders());
    } else {
      valid = Http.connectableHttpUrl(httpConnectionExecutionCapability.fetchConnectableUrl());
    }
    return CapabilityResponse.builder().delegateCapability(httpConnectionExecutionCapability).validated(valid).build();
  }

  @Override
  public CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters) {
    CapabilitySubjectPermissionBuilder builder = CapabilitySubjectPermission.builder();
    if (parameters.getCapabilityCase() != CapabilityParameters.CapabilityCase.HTTP_CONNECTION_PARAMETERS) {
      return builder.permissionResult(PermissionResult.DENIED).build();
    }
    HttpConnectionParameters httpConnectionParameters = parameters.getHttpConnectionParameters();
    // Use isNotEmpty since proto3 initializes an emptyList when headers are not added.
    // https://github.com/protocolbuffers/protobuf/blob/v3.12.0/docs/field_presence.md
    if (EmptyPredicate.isNotEmpty(httpConnectionParameters.getHeadersList())) {
      return builder
          .permissionResult(
              Http.connectableHttpUrlWithHeaders(httpConnectionParameters.getUrl(),
                  httpConnectionParameters.getHeadersList()
                      .stream()
                      .map(entry -> KeyValuePair.builder().key(entry.getKey()).value(entry.getValue()).build())
                      .collect(Collectors.toList()))
                  ? PermissionResult.ALLOWED
                  : PermissionResult.DENIED)
          .build();
    } else {
      return builder
          .permissionResult(Http.connectableHttpUrl(parameters.getHttpConnectionParameters().getUrl())
                  ? PermissionResult.ALLOWED
                  : PermissionResult.DENIED)
          .build();
    }
  }
}
