package io.harness.cvng.activity.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.DeploymentActivityDTO;
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
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

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
    verificationJobInstanceBuilder.startTime(this.getVerificationStartTime());
  }

  @Override
  public void validateActivityParams() {
    Preconditions.checkNotNull(deploymentTag, "Deployment tag can not be null");
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
    return Instant.ofEpochMilli(this.verificationStartTime);
  }

  public static class DeploymentActivityUpdatableEntity
      extends ActivityUpdatableEntity<DeploymentActivity, DeploymentActivity> {
    @Override
    public Class getEntityClass() {
      return DeploymentActivity.class;
    }

    @Override
    public Query<DeploymentActivity> populateKeyQuery(Query<DeploymentActivity> query, DeploymentActivity changeEvent) {
      throw new UnsupportedOperationException("DeploymentActivity events have no unique key");
    }

    @Override
    public void setUpdateOperations(
        UpdateOperations<DeploymentActivity> updateOperations, DeploymentActivity activity) {
      setCommonUpdateOperations(updateOperations, activity);
    }
  }
}
