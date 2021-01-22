package io.harness.cvng.statemachine.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "AnalysisOrchestratorKeys")
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "analysisOrchestrators", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class AnalysisOrchestrator implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  @FdIndex private String verificationTaskId;
  @Builder.Default private List<AnalysisStateMachine> analysisStateMachineQueue = new ArrayList<>();
  private AnalysisStatus status;
  private long createdAt;
  private long lastUpdatedAt;

  @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusDays(7).toInstant());
}
