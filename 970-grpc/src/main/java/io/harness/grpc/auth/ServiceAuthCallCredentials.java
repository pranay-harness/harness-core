/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.auth;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.grpc.utils.GrpcAuthUtils;
import io.harness.security.ServiceTokenGenerator;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import java.util.concurrent.Executor;
import lombok.Getter;

@Getter
@Singleton
public final class ServiceAuthCallCredentials extends CallCredentials {
  private final ServiceTokenGenerator serviceTokenGenerator;
  private final String serviceSecret;
  private final String serviceId;

  public ServiceAuthCallCredentials(
      String serviceSecret, ServiceTokenGenerator serviceTokenGenerator, String serviceId) {
    Preconditions.checkArgument(isNotBlank(serviceSecret), "Service secret cannot be null");
    this.serviceSecret = serviceSecret.trim();
    this.serviceTokenGenerator = serviceTokenGenerator;
    this.serviceId = serviceId;
  }

  @Override
  public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier metadataApplier) {
    String token = serviceTokenGenerator.getServiceToken(serviceSecret);
    Metadata headers = new Metadata();
    GrpcAuthUtils.setServiceAuthDetailsInRequest(serviceId, token, headers);
    metadataApplier.apply(headers);
  }

  @Override
  public void thisUsesUnstableApi() {
    // No impl at present
  }
}
