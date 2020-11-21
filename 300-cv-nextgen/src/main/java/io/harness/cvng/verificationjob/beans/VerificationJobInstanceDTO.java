package io.harness.cvng.verificationjob.beans;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;

import io.harness.cvng.core.utils.DateTimeUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
public class VerificationJobInstanceDTO {
  @NotNull String verificationJobIdentifier;
  @Getter(AccessLevel.NONE) Long deploymentStartTimeMs;
  @Getter(AccessLevel.NONE) @NotNull Long verificationTaskStartTimeMs;
  @Getter(AccessLevel.NONE) Long dataCollectionDelayMs;
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
    if (deploymentStartTimeMs == null) {
      return this.getVerificationStartTime().minus(Duration.ofMinutes(5));
    }
    return DateTimeUtils.roundDownTo1MinBoundary(Instant.ofEpochMilli(this.deploymentStartTimeMs));
  }

  @JsonIgnore
  public Instant getVerificationStartTime() {
    return DateTimeUtils.roundDownTo1MinBoundary(Instant.ofEpochMilli(this.verificationTaskStartTimeMs));
  }

  // TODO: add map to pass runtime values.
}
