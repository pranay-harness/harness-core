package io.harness.cvng.statemachine.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.OffsetDateTime;
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
@FieldNameConstants(innerTypeName = "AnalysisStateMachineKeys")
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "analysisStateMachines", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class AnalysisStateMachine implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("execute_query_idx")
                 .field(AnalysisStateMachineKeys.verificationTaskId)
                 .field(AnalysisStateMachineKeys.status)
                 .build())
        .build();
  }

  private String accountId;

  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  private String verificationTaskId;
  private AnalysisState currentState;
  private List<AnalysisState> completedStates;
  private AnalysisStatus status;

  private long nextAttemptTime;

  @FdTtlIndex @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plusDays(14).toInstant());
}
