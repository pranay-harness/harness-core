package io.harness.cvng.core.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.core.utils.DateTimeUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import javax.validation.constraints.NotNull;

@JsonTypeName("DEPLOYMENT")
@Data
@FieldNameConstants(innerTypeName = "DeploymentActivityKeys")
@Builder
@AllArgsConstructor
public class DeploymentActivity extends Activity {
  Long dataCollectionDelayMs;
  Set<String> oldVersionHosts;
  Set<String> newVersionHosts;
  Integer newHostsTrafficSplitPercentage;
  String deploymentTag;
  @Getter(AccessLevel.NONE) @NotNull Long verificationStartTime;

  @Override
  public ActivityType getType() {
    return ActivityType.DEPLOYMENT;
  }

  @Override
  public void validateActivityParams() {
    Preconditions.checkNotNull(getVerificationJobRuntimeDetails(),
        "Verification job details cannot be "
            + "empty for a deployment activity");
    getVerificationJobRuntimeDetails().forEach(verificationJob -> {
      Preconditions.checkNotNull(isEmpty(verificationJob.getVerificationJobIdentifier()),
          "The verification job identifier is a required parameter for deployment activities");
    });
  }

  @JsonIgnore
  public Duration getDataCollectionDelay() {
    if (dataCollectionDelayMs == null) {
      return DATA_COLLECTION_DELAY;
    } else {
      return Duration.ofMillis(dataCollectionDelayMs);
    }
  }

  @JsonIgnore
  public Instant getVerificationStartTime() {
    return DateTimeUtils.roundDownTo1MinBoundary(Instant.ofEpochMilli(this.verificationStartTime));
  }
}
