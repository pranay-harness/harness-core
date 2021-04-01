package io.harness.testing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateTaskStressTestConfiguration extends Configuration {
  // where the setup location is made
  @JsonProperty("setupLocation") private String setupLocation;
  @JsonProperty("serviceSecret") private String serviceSecret;
  @JsonProperty("target") private String target;
  @JsonProperty("authority") private String authority;
}
