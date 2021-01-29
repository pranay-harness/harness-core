package io.harness;

import io.harness.mongo.MongoConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import io.dropwizard.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class ChangeDataCaptureServiceConfig extends Configuration {
  @JsonProperty("harness-mongo") private MongoConfig harnessMongo = MongoConfig.builder().build();
  @JsonProperty("events-mongo") private MongoConfig eventsMongo = MongoConfig.builder().build();
}
