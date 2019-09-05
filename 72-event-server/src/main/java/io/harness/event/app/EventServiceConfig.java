package io.harness.event.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.grpc.server.Connector;
import io.harness.mongo.MongoConfig;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class EventServiceConfig {
  @JsonProperty("mongo") private MongoConfig mongoConnectionFactory = MongoConfig.builder().build();

  @Singular private List<Connector> connectors;
}
