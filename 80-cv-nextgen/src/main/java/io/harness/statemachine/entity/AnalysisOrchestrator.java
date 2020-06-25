package io.harness.statemachine.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "AnalysisOrchestratorKeys")
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "analysisOrchestrators")
@HarnessEntity(exportable = true)

public class AnalysisOrchestrator implements PersistentEntity, UuidAware {
  @Id private String uuid;
  @Indexed private String cvConfigId;
  private List<AnalysisStateMachine> analysisStateMachineQueue;
  private AnalysisStatus status;
}
