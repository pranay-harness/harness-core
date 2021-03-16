package io.harness.cvng.beans.newrelic;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewRelicApplication {
  private String applicationName;
  private long applicationId;
}
