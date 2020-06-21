package software.wings.delegatetasks.validation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.Indexed;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultKeys;

import java.time.OffsetDateTime;
import java.util.Date;
import javax.validation.constraints.NotNull;

@Data
@Builder
@Entity(value = "delegateConnectionResults", noClassnameStored = true)
@HarnessEntity(exportable = false)

@Index(name = "delegateConnectionResultsIdx",
    fields =
    {
      @Field(DelegateConnectionResultKeys.accountId)
      , @Field(DelegateConnectionResultKeys.delegateId), @Field(DelegateConnectionResultKeys.criteria)
    },
    options = @IndexOptions(unique = true))
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "DelegateConnectionResultKeys")
public class DelegateConnectionResult implements PersistentEntity, UuidAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;

  @NotNull private long lastUpdatedAt;

  @NotEmpty private String accountId;
  @Indexed @NotEmpty private String delegateId;
  @Indexed @NotEmpty private String criteria;
  private boolean validated;
  private long duration;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) @Default private Date validUntil = getValidUntilTime();

  public static Date getValidUntilTime() {
    return Date.from(OffsetDateTime.now().plusDays(30).toInstant());
  }
}
