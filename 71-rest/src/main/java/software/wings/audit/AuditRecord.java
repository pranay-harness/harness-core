package software.wings.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import software.wings.audit.AuditRecord.AuditRecordKeys;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@OwnedBy(PL)
@Data
@Builder
@Entity(value = "entityAuditRecords", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "AuditRecordKeys")

@CdIndex(name = "entityRecordIndex_1",
    fields =
    { @Field(AuditRecordKeys.auditHeaderId)
      , @Field(value = AuditRecordKeys.createdAt, type = IndexType.DESC), })
public class AuditRecord
    implements PersistentEntity, CreatedAtAware, UuidAware, PersistentRegularIterable, AccountAccess {
  @Id @NotNull private String uuid;
  @NotEmpty String auditHeaderId;
  @NotNull EntityAuditRecord entityAuditRecord;
  private long createdAt;
  @Setter @FdIndex private Long nextIteration;
  private String accountId;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }
}
