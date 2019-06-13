package io.harness.migrator.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.mongo.MongoConfig;
import lombok.Data;

@Data
public class MigratorConfiguration {
  @JsonProperty("mongo") private MongoConfig mongoConnectionFactory = MongoConfig.builder().build();
}
