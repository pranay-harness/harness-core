/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.platform.notification;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.mongo.MongoConfig;
import io.harness.notification.SeedDataConfiguration;
import io.harness.notification.SmtpConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Value
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationServiceConfiguration {
  @JsonProperty("mongo") MongoConfig mongoConfig;
  @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty("seedDataConfiguration") private SeedDataConfiguration seedDataConfiguration;
  @JsonProperty("delegateServiceGrpcConfig") private GrpcClientConfig delegateServiceGrpcConfig;
  @JsonProperty(value = "useDms", defaultValue = "false") private boolean useDms;
  @JsonProperty("dmsGrpcClient") private GrpcClientConfig dmsGrpcClient;
}
