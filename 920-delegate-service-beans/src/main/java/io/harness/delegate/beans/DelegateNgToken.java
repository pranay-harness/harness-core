package io.harness.delegate.beans;

import static java.time.Duration.ofDays;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityIdentifier;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.NameAndValueAccess;
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
@Entity(value = "delegateTokensNG", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "DelegateNgTokenKeys")
@OwnedBy(HarnessTeam.DEL)
@StoreIn(DbAliases.ALL)
public class DelegateNgToken
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, NameAndValueAccess {
  public static final Duration TTL = ofDays(30);

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .field(DelegateNgTokenKeys.accountId)
                 .field(DelegateNgTokenKeys.owner)
                 .field(DelegateNgTokenKeys.name)
                 .unique(true)
                 .name("byAccountOwnerAndName")
                 .build())
        .add(CompoundMongoIndex.builder()
                 .field(DelegateNgTokenKeys.accountId)
                 .field(DelegateNgTokenKeys.owner)
                 .field(DelegateNgTokenKeys.status)
                 .name("byAccountAndStatus")
                 .build())
        .build();
  }

  @Id @NotNull private String uuid;
  @NotEmpty private String accountId;
  @NotEmpty private String name;
  private DelegateEntityOwner owner;
  private EmbeddedUser createdBy;
  private long createdAt;
  private DelegateTokenStatus status;
  private String value;
  @EntityIdentifier private String identifier;
  @FdTtlIndex private Date validUntil;
}
