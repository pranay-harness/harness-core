package io.harness.cvng.activity.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.DeploymentActivityDTO;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("DEPLOYMENT")
@FieldNameConstants(innerTypeName = "DeploymentActivityKeys")
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
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
  public void fromDTO(ActivityDTO activityDTO) {
    Preconditions.checkState(activityDTO instanceof DeploymentActivityDTO);
    DeploymentActivityDTO deploymentActivityDTO = (DeploymentActivityDTO) activityDTO;
    setDataCollectionDelayMs(deploymentActivityDTO.getDataCollectionDelayMs());
    setOldVersionHosts(deploymentActivityDTO.getOldVersionHosts());
    setNewVersionHosts(deploymentActivityDTO.getNewVersionHosts());
    setNewHostsTrafficSplitPercentage(deploymentActivityDTO.getNewHostsTrafficSplitPercentage());
    setDeploymentTag(deploymentActivityDTO.getDeploymentTag());
    setVerificationStartTime(deploymentActivityDTO.getVerificationStartTime());
    setType(ActivityType.DEPLOYMENT);
    addCommonFields(activityDTO);
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstanceBuilder verificationJobInstanceBuilder) {
    verificationJobInstanceBuilder.oldVersionHosts(this.getOldVersionHosts());
    verificationJobInstanceBuilder.newVersionHosts(this.getNewVersionHosts());
    verificationJobInstanceBuilder.newHostsTrafficSplitPercentage(this.getNewHostsTrafficSplitPercentage());
    verificationJobInstanceBuilder.dataCollectionDelay(this.getDataCollectionDelay());

    // Set the properties needed for a health verification instance
    Instant postActivityStart = DateTimeUtils.roundDownTo5MinBoundary(getVerificationStartTime());
    Instant preActivityStart = postActivityStart.minus(verificationJobInstanceBuilder.getResolvedJob().getDuration());
    // TODO: we can probably get rid of this logic by moving postActivityStart, preActivityStart inside the job
    if (!VerificationJobType.getDeploymentJobTypes().contains(
            verificationJobInstanceBuilder.getResolvedJob().getType())) {
      verificationJobInstanceBuilder.startTime(preActivityStart);
    } else {
      verificationJobInstanceBuilder.startTime(this.getVerificationStartTime());
    }
    // TODO: These should be inferred from startTime and probably should be part of HealthVerification as methods
    verificationJobInstanceBuilder.preActivityVerificationStartTime(preActivityStart);
    verificationJobInstanceBuilder.postActivityVerificationStartTime(postActivityStart);
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

  @Override
  public boolean deduplicateEvents() {
    return false;
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
    return this.verificationStartTime == null
        ? null
        : DateTimeUtils.roundDownTo1MinBoundary(Instant.ofEpochMilli(this.verificationStartTime));
  }
}
