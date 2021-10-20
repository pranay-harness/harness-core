package io.harness.delegate.beans;

import static java.time.Duration.ofDays;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "delegateTokens", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "DelegateTokenKeys")
@OwnedBy(HarnessTeam.DEL)
@StoreIn(DbAliases.ALL)
public final class DelegateToken implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware {
  public static final Duration TTL = ofDays(30);

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .field(DelegateTokenKeys.accountId)
                 .field(DelegateTokenKeys.name)
                 .unique(true)
                 .name("byAccountAndName")
                 .build())
        .add(CompoundMongoIndex.builder()
                 .field(DelegateTokenKeys.accountId)
                 .field(DelegateTokenKeys.status)
                 .name("byAccountAndStatus")
                 .build())
        .build();
  }

  @Id @NotNull private String uuid;
  @NotEmpty private String accountId;
  @NotEmpty private String name;
  private EmbeddedUser createdBy;
  private long createdAt;
  private DelegateTokenStatus status;
  private String value;

  @FdTtlIndex private Date validUntil;
}
