/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.app;

import io.harness.grpc.server.Connector;
import io.harness.mongo.MongoConfig;
import io.harness.secret.ConfigSecret;
import io.harness.secret.SecretsConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class EventServiceConfig {
  @Builder.Default
  @JsonProperty("harness-mongo")
  @ConfigSecret
  private MongoConfig harnessMongo = MongoConfig.builder().build();
  @Builder.Default
  @JsonProperty("events-mongo")
  @ConfigSecret
  private MongoConfig eventsMongo = MongoConfig.builder().build();
  @JsonProperty("secretsConfiguration") private SecretsConfiguration secretsConfiguration;

  @Singular private List<Connector> connectors;
}
