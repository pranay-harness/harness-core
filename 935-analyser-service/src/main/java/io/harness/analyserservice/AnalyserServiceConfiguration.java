package io.harness.analyserservice;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.mongo.MongoConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(PIPELINE)
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyserServiceConfiguration extends Configuration {
  @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @JsonProperty("executionTimeLimitMillis") private Long executionTimeLimitMillis;
  @JsonProperty("manyEntriesAlertFactor") private Integer manyEntriesAlertFactor;
}
