package io.harness.event.reconciliation.deployment;

import io.harness.event.reconciliation.deployment.DeploymentReconRecord.DeploymentReconRecordKeys;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;

import java.time.OffsetDateTime;
import java.util.Date;

@Data
@Builder
@FieldNameConstants(innerTypeName = "DeploymentReconRecordKeys")
@ToString
@Entity(value = "deploymentReconciliation", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "accountId_durationEndTs"),
      fields = { @Field(DeploymentReconRecordKeys.accountId)
                 , @Field(DeploymentReconRecordKeys.durationEndTs) })
  ,
      @Index(options = @IndexOptions(name = "accountId_reconciliationStatus"), fields = {
        @Field(DeploymentReconRecordKeys.accountId), @Field(DeploymentReconRecordKeys.reconciliationStatus)
      }),
})
public class DeploymentReconRecord implements PersistentEntity, UuidAware {
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
