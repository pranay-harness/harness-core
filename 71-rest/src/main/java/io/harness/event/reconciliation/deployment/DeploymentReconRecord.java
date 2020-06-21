package io.harness.event.reconciliation.deployment;

import io.harness.annotation.HarnessEntity;
import io.harness.event.reconciliation.deployment.DeploymentReconRecord.DeploymentReconRecordKeys;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.Indexed;
import io.harness.mongo.index.Indexes;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.OffsetDateTime;
import java.util.Date;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DeploymentReconRecordKeys")
@ToString
@Entity(value = "deploymentReconciliation", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Indexes({
  @Index(name = "accountId_durationEndTs",
      fields = { @Field(DeploymentReconRecordKeys.accountId)
                 , @Field(DeploymentReconRecordKeys.durationEndTs) })
  ,
      @Index(name = "accountId_reconciliationStatus", fields = {
        @Field(DeploymentReconRecordKeys.accountId), @Field(DeploymentReconRecordKeys.reconciliationStatus)
      }),
})
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
  @Default
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date ttl = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
}
