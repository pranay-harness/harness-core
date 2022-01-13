/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.statemachine.entities;

import static io.harness.cvng.CVConstants.STATE_MACHINE_IGNORE_MINUTES;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
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
@StoreIn(DbAliases.CVNG)
public final class AnalysisStateMachine
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("execute_query_idx")
                 .field(AnalysisStateMachineKeys.verificationTaskId)
                 .field(AnalysisStateMachineKeys.status)
                 .build())
        .add(SortCompoundMongoIndex.builder()
                 .name("verificationTaskIdQueryIdx")
                 .unique(false)
                 .field(AnalysisStateMachineKeys.verificationTaskId)
                 .descSortField(AnalysisStateMachineKeys.createdAt)
                 .build())
        .build();
  }

  @Id private String uuid;
  @FdIndex private String accountId;
  private long createdAt;
  private long lastUpdatedAt;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  private String verificationTaskId;
  private AnalysisState currentState;
  private List<AnalysisState> completedStates;
  @FdIndex private AnalysisStatus status;

  private long nextAttemptTime;

  private Integer stateMachineIgnoreMinutes;

  public Integer getStateMachineIgnoreMinutes() {
    if (stateMachineIgnoreMinutes == null) {
      stateMachineIgnoreMinutes = STATE_MACHINE_IGNORE_MINUTES;
    }
    return stateMachineIgnoreMinutes;
  }

  @FdTtlIndex @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plusDays(14).toInstant());
}
