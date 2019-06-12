package io.harness.timescaledb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
public class TimeScaleDBConfig {
  @JsonProperty(defaultValue = "jdbc:postgresql://localhost:5432/harness")
  @Builder.Default
  @NotEmpty
  private String timescaledbUrl;
  private String timescaledbUsername;
  private String timescaledbPassword;
}
