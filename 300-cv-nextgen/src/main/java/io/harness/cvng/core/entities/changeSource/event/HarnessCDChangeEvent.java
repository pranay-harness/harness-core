package io.harness.cvng.core.entities.changeSource.event;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "HarnessCDChangeEventKeys")
public class HarnessCDChangeEvent extends ChangeEvent {
  long deploymentStartTime;
  long deploymentEndTime;
  String planExecutionId;
  String pipelineId;
  String stageStepId;
  String stageId;
  String artifactType;
  String artifactTag;
  String status;

  public static class HarnessCDChangeEventUpdatableEntity
      extends ChangeEventUpdatableEntity<HarnessCDChangeEvent, HarnessCDChangeEvent> {
    @Override
    public Class getEntityClass() {
      return HarnessCDChangeEvent.class;
    }

    public Query<HarnessCDChangeEvent> populateKeyQuery(
        Query<HarnessCDChangeEvent> query, HarnessCDChangeEvent changeEvent) {
      return super.populateKeyQuery(query, changeEvent)
          .filter(HarnessCDChangeEventKeys.planExecutionId, changeEvent.getPlanExecutionId())
          .filter(HarnessCDChangeEventKeys.stageStepId, changeEvent.getStageStepId());
    }

    @Override
    public void setUpdateOperations(
        UpdateOperations<HarnessCDChangeEvent> updateOperations, HarnessCDChangeEvent harnessCDChangeSource) {
      setCommonUpdateOperations(updateOperations, harnessCDChangeSource);
      updateOperations.set(HarnessCDChangeEventKeys.status, harnessCDChangeSource.getStatus())
          .set(HarnessCDChangeEventKeys.deploymentStartTime, harnessCDChangeSource.getDeploymentStartTime())
          .set(HarnessCDChangeEventKeys.deploymentEndTime, harnessCDChangeSource.getDeploymentEndTime())
          .set(HarnessCDChangeEventKeys.planExecutionId, harnessCDChangeSource.getPlanExecutionId())
          .set(HarnessCDChangeEventKeys.pipelineId, harnessCDChangeSource.getPipelineId())
          .set(HarnessCDChangeEventKeys.stageStepId, harnessCDChangeSource.getStageStepId())
          .set(HarnessCDChangeEventKeys.stageId, harnessCDChangeSource.getStageId())
          .set(HarnessCDChangeEventKeys.artifactType, harnessCDChangeSource.getArtifactType())
          .set(HarnessCDChangeEventKeys.artifactTag, harnessCDChangeSource.getArtifactTag())
          .set(HarnessCDChangeEventKeys.status, harnessCDChangeSource.getStatus());
    }
  }
}
