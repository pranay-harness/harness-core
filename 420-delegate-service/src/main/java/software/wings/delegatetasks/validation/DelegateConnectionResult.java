package software.wings.delegatetasks.validation;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "delegateConnectionResults", noClassnameStored = true)
@HarnessEntity(exportable = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "DelegateConnectionResultKeys")
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
@StoreIn("dms")
public class DelegateConnectionResult implements PersistentEntity, UuidAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("delegateConnectionResultsIdx")
                 .unique(true)
                 .field(DelegateConnectionResultKeys.accountId)
                 .field(DelegateConnectionResultKeys.delegateId)
                 .field(DelegateConnectionResultKeys.criteria)
                 .build())
        .build();
  }

  @Id private String uuid;

  @NotNull private long lastUpdatedAt;

  @NotEmpty private String accountId;
  @FdIndex @NotEmpty private String delegateId;
  @FdIndex @NotEmpty private String criteria;
  private boolean validated;
  private long duration;

  @FdTtlIndex @Default private Date validUntil = getValidUntilTime();

  public static Date getValidUntilTime() {
    return Date.from(OffsetDateTime.now().plusDays(30).toInstant());
  }
}
