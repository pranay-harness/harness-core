package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage.LastReceivedPublishedMessageKeys;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "lastReceivedPublishedMessage", noClassnameStored = true)
@NgUniqueIndex(name = "no_dup",
    fields =
    { @Field(LastReceivedPublishedMessageKeys.accountId)
      , @Field(LastReceivedPublishedMessageKeys.identifier) })
@FieldNameConstants(innerTypeName = "LastReceivedPublishedMessageKeys")
public final class LastReceivedPublishedMessage
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  @NotEmpty String accountId;
  @NotEmpty String identifier;
  long lastReceivedAt;
  long createdAt;
  long lastUpdatedAt;
}
