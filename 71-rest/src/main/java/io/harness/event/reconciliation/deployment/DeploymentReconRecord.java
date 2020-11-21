package io.harness.event.reconciliation.deployment;

import io.harness.annotation.HarnessEntity;
import io.harness.event.reconciliation.deployment.DeploymentReconRecord.DeploymentReconRecordKeys;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import java.time.OffsetDateTime;
import java.util.Date;
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

@CdIndex(name = "accountId_durationEndTs",
    fields = { @Field(DeploymentReconRecordKeys.accountId)
               , @Field(DeploymentReconRecordKeys.durationEndTs) })
@CdIndex(name = "accountId_reconciliationStatus",
    fields = { @Field(DeploymentReconRecordKeys.accountId)
               , @Field(DeploymentReconRecordKeys.reconciliationStatus) })
public class DeploymentReconRecord implements PersistentEntity, UuidAware, AccountAccess {
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
