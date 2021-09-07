package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@FieldNameConstants(innerTypeName = "HarnessCDActivityKeys")
@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HarnessCDActivity extends Activity {
  String planExecutionId;
  String pipelineId;
  String stageStepId;
  String stageId;
  String artifactType;
  String artifactTag;
  String deploymentStatus;

  @Override
  public ActivityType getType() {
    return ActivityType.HARNESS_CD;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    throw new UnsupportedOperationException("Harness CD Events can be transformed only from ChangeEventDTO");
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstanceBuilder verificationJobInstanceBuilder) {
    throw new UnsupportedOperationException("Harness CD Events shouldn't have verification jobs");
  }

  @Override
  public void validateActivityParams() {}

  @Override
  public boolean deduplicateEvents() {
    return true;
  }

  public static class HarnessCDActivityUpdatableEntity
      extends ActivityUpdatableEntity<HarnessCDActivity, HarnessCDActivity> {
    @Override
    public Class getEntityClass() {
      return HarnessCDActivity.class;
    }

    public Query<HarnessCDActivity> populateKeyQuery(Query<HarnessCDActivity> query, HarnessCDActivity activity) {
      return super.populateKeyQuery(query, activity)
          .filter(HarnessCDActivityKeys.planExecutionId, activity.getPlanExecutionId())
          .filter(HarnessCDActivityKeys.stageStepId, activity.getStageStepId());
    }

    @Override
    public void setUpdateOperations(
        UpdateOperations<HarnessCDActivity> updateOperations, HarnessCDActivity harnessCDActivity) {
      setCommonUpdateOperations(updateOperations, harnessCDActivity);
      updateOperations.set(HarnessCDActivityKeys.planExecutionId, harnessCDActivity.getPlanExecutionId())
          .set(HarnessCDActivityKeys.pipelineId, harnessCDActivity.getPipelineId())
          .set(HarnessCDActivityKeys.stageStepId, harnessCDActivity.getStageStepId())
          .set(HarnessCDActivityKeys.stageId, harnessCDActivity.getStageId())
          .set(HarnessCDActivityKeys.artifactType, harnessCDActivity.getArtifactType())
          .set(HarnessCDActivityKeys.artifactTag, harnessCDActivity.getArtifactTag())
          .set(HarnessCDActivityKeys.deploymentStatus, harnessCDActivity.getDeploymentStatus());
    }
  }
}
