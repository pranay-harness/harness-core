package io.harness.event.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.mongo.MongoConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventServiceConfig {
  @JsonProperty("mongo") private MongoConfig mongoConnectionFactory = MongoConfig.builder().build();
  private int plainTextPort;
  private int securePort;
  private String keyFilePath;
  private String certFilePath;
}
