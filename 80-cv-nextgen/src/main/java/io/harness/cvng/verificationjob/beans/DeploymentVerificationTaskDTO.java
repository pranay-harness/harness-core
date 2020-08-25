package io.harness.cvng.verificationjob.beans;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.cvng.core.utils.DateTimeUtils;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class DeploymentVerificationTaskDTO {
  @NotNull String verificationJobIdentifier;
  Long deploymentStartTimeMs;
  @NotNull Long verificationTaskStartTimeMs;
  Long dataCollectionDelayMs;
  Set<String> oldVersionHosts;
  Set<String> newVersionHosts;
  Integer newHostsTrafficSplitPercentage;

  @JsonIgnore
  public Duration getDataCollectionDelay() {
    if (dataCollectionDelayMs == null) {
      return DATA_COLLECTION_DELAY;
    } else {
      return Duration.ofMillis(dataCollectionDelayMs);
    }
  }

  @JsonIgnore
  public Instant getDeploymentStartTime() {
    if (getDeploymentStartTimeMs() == null) {
      return this.getVerificationStartTime().minus(Duration.ofMinutes(5));
    }
    return DateTimeUtils.roundDownTo1MinBoundary(Instant.ofEpochMilli(this.getDeploymentStartTimeMs()));
  }

  @JsonIgnore
  public Instant getVerificationStartTime() {
    return DateTimeUtils.roundDownTo1MinBoundary(Instant.ofEpochMilli(this.getVerificationTaskStartTimeMs()));
  }

  // TODO: add map to pass runtime values.
}
