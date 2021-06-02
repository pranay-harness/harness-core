package io.harness.cvng.beans.stackdriver;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@OwnedBy(CV)
public abstract class StackdriverLogRequest extends StackdriverRequest {
  @Override
  public String getBaseUrl() {
    return "https://logging.googleapis.com/v2/";
  }
}
