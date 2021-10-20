package io.harness.event.reconciliation.deployment;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DeploymentReconRecordKeys")
@ToString
@Entity(value = "deploymentReconciliation", noClassnameStored = true)
@HarnessEntity(exportable = false)
public final class DeploymentReconRecord implements PersistentEntity, UuidAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_durationEndTs")
                 .field(DeploymentReconRecordKeys.accountId)
                 .field(DeploymentReconRecordKeys.durationEndTs)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("accountId_reconciliationStatus")
                 .field(DeploymentReconRecordKeys.accountId)
                 .field(DeploymentReconRecordKeys.reconciliationStatus)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String accountId;
  private long durationStartTs;
  private long durationEndTs;
  private DetectionStatus detectionStatus;
  private ReconciliationStatus reconciliationStatus;
  private ReconcilationAction reconcilationAction;
  private long reconStartTs;
  private long reconEndTs;
  @Default @FdTtlIndex private Date ttl = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
}
