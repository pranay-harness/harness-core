package io.harness.perpetualtask.internal;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentFibonacciIterable;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskClientContext.PerpetualTaskClientContextKeys;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.PerpetualTaskUnassignedReason;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "PerpetualTaskRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "perpetualTask", noClassnameStored = true)
@CdIndex(name = "assignerIterator",
    fields = { @Field(PerpetualTaskRecordKeys.state)
               , @Field(PerpetualTaskRecordKeys.assignerIterations) })
@HarnessEntity(exportable = false)
@Slf4j
public class PerpetualTaskRecord implements PersistentEntity, UuidAware, PersistentRegularIterable,
                                            PersistentFibonacciIterable, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  @FdIndex String accountId;
  String perpetualTaskType;
  PerpetualTaskClientContext clientContext;
  long intervalSeconds;
  long timeoutMillis;
  @FdIndex String delegateId;
  String taskDescription;
  PerpetualTaskState state;
  PerpetualTaskUnassignedReason unassignedReason;
  long lastHeartbeat;

  List<Long> assignerIterations;
  @FdIndex long rebalanceIteration;

  long createdAt;
  long lastUpdatedAt;

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (PerpetualTaskRecordKeys.assignerIterations.equals(fieldName)) {
      return isEmpty(assignerIterations) ? null : assignerIterations.get(0);
    }
    if (PerpetualTaskRecordKeys.rebalanceIteration.equals(fieldName)) {
      return rebalanceIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public List<Long> recalculateNextIterations(String fieldName, boolean skipMissed, long throttled) {
    if (PerpetualTaskRecordKeys.assignerIterations.equals(fieldName)) {
      if (assignerIterations == null) {
        assignerIterations = new ArrayList<>();
      }
      if (recalculateTimestamps(assignerIterations, skipMissed, throttled, ofSeconds(30), ofMinutes(30))) {
        return assignerIterations;
      }
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (PerpetualTaskRecordKeys.rebalanceIteration.equals(fieldName)) {
      this.rebalanceIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public static final class PerpetualTaskRecordKeys {
    private PerpetualTaskRecordKeys() {}
    public static final String client_context_last_updated =
        clientContext + "." + PerpetualTaskClientContextKeys.lastContextUpdated;
    public static final String client_params = clientContext + "." + PerpetualTaskClientContextKeys.clientParams;
    public static final String task_parameters = clientContext + "." + PerpetualTaskClientContextKeys.executionBundle;
  }
}
