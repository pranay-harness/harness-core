/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.app;

import io.harness.grpc.server.Connector;
import io.harness.mongo.MongoConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class EventServiceConfig {
  @Builder.Default @JsonProperty("harness-mongo") private MongoConfig harnessMongo = MongoConfig.builder().build();
  @Builder.Default @JsonProperty("events-mongo") private MongoConfig eventsMongo = MongoConfig.builder().build();

  @Singular private List<Connector> connectors;
}
