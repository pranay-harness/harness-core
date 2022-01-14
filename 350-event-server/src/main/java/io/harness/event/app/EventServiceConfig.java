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
