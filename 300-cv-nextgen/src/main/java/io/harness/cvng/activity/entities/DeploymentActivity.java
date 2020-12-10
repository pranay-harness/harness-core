package io.harness.cvng.activity.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.DeploymentActivityDTO;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

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
    addCommonFileds(activityDTO);
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstance verificationJobInstance) {
    verificationJobInstance.setOldVersionHosts(this.getOldVersionHosts());
    verificationJobInstance.setNewVersionHosts(this.getNewVersionHosts());
    verificationJobInstance.setNewHostsTrafficSplitPercentage(this.getNewHostsTrafficSplitPercentage());
    verificationJobInstance.setDataCollectionDelay(this.getDataCollectionDelay());

    // Set the properties needed for a health verification instance
    Instant roundedDownTime = DateTimeUtils.roundDownTo5MinBoundary(getActivityStartTime());
    Instant preactivityStart = roundedDownTime.minus(verificationJobInstance.getResolvedJob().getDuration());

    if (!VerificationJobType.getDeploymentJobTypes().contains(verificationJobInstance.getResolvedJob().getType())) {
      verificationJobInstance.setStartTime(preactivityStart);
    } else {
      verificationJobInstance.setStartTime(this.getVerificationStartTime());
    }
    verificationJobInstance.setPreActivityVerificationStartTime(preactivityStart);
    verificationJobInstance.setPostActivityVerificationStartTime(roundedDownTime);
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
    return this.verificationStartTime == null
        ? null
        : DateTimeUtils.roundDownTo1MinBoundary(Instant.ofEpochMilli(this.verificationStartTime));
  }
}
