package io.harness.cvng.statemachine.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;
import java.util.List;

@CdIndex(name = "state_machine_index", fields = { @Field("cvConfigId")
                                                  , @Field(value = "status") })
@Data
@Builder
@FieldNameConstants(innerTypeName = "AnalysisStateMachineKeys")
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "analysisStateMachines", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class AnalysisStateMachine implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  @FdIndex private String cvConfigId;
  @FdIndex private String verificationTaskId;
  private AnalysisState currentState;
  private List<AnalysisState> completedStates;
  private AnalysisStatus status;

  private long nextAttemptTime;
}
