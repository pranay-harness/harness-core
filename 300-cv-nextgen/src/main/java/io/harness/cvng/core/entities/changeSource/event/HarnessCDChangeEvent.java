package io.harness.cvng.core.entities.changeSource.event;

import io.harness.pms.contracts.execution.Status;

import java.time.Instant;
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
  Instant deploymentStartTime;
  Instant deploymentEndTime;
  String executionId;
  String stageId;
  Status status;

  public static class HarnessCDChangeEventUpdatableEntity
      extends ChangeEventUpdatableEntity<HarnessCDChangeEvent, HarnessCDChangeEvent> {
    @Override
    public Class getEntityClass() {
      return HarnessCDChangeEvent.class;
    }

    public Query<HarnessCDChangeEvent> populateKeyQuery(
        Query<HarnessCDChangeEvent> query, HarnessCDChangeEvent changeEvent) {
      return super.populateKeyQuery(query, changeEvent)
          .filter(HarnessCDChangeEventKeys.executionId, changeEvent.getExecutionId())
          .filter(HarnessCDChangeEventKeys.stageId, changeEvent.getStageId());
    }

    @Override
    public void setUpdateOperations(
        UpdateOperations<HarnessCDChangeEvent> updateOperations, HarnessCDChangeEvent harnessCDChangeSource) {
      setCommonUpdateOperations(updateOperations, harnessCDChangeSource);
      updateOperations.set(HarnessCDChangeEventKeys.status, harnessCDChangeSource.getStatus())
          .set(HarnessCDChangeEventKeys.deploymentEndTime, harnessCDChangeSource.getDeploymentEndTime());
    }
  }
}
