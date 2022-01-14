package io.harness.cvng.beans.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicApplication {
  private String applicationName;
  private long applicationId;
}
