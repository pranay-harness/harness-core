package software.wings.beans;

import io.harness.distribution.idempotence.IdempotentResult;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "IdempotentKeys")
@Entity(value = "idempotent_locks", noClassnameStored = true)
public class Idempotent implements PersistentEntity {
  @Id private String uuid;

  public static final String TENTATIVE = "tentative";
  public static final String SUCCEEDED = "succeeded";

  private String state;
  private List<IdempotentResult> result;

  @Default
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(3).toInstant());
}
