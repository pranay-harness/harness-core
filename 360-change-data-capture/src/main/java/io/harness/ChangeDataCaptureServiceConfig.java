package io.harness;

import io.harness.mongo.MongoConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Value;

@Data
@Value
public class ChangeDataCaptureServiceConfig {
  @JsonProperty("harness-mongo") private MongoConfig harnessMongo;
  @JsonProperty("events-mongo") private MongoConfig eventsMongo;
}
