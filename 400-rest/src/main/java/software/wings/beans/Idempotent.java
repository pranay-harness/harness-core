package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.distribution.idempotence.IdempotentResult;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@FieldNameConstants(innerTypeName = "IdempotentKeys")
@Entity(value = "idempotent_locks", noClassnameStored = true)
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.CG_MANAGER)
public class Idempotent implements PersistentEntity {
  @Id private String uuid;

  public static final String TENTATIVE = "tentative";
  public static final String SUCCEEDED = "succeeded";

  private String state;
  private List<IdempotentResult> result;

  @Default @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusDays(3).toInstant());
}
