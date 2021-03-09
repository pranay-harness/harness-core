package io.harness.ccm.communication.entities;

import com.google.common.collect.ImmutableList;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
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
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CECommunicationsKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceCommunications", noClassnameStored = true)
public class CECommunications implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("account_email_type")
                 .unique(true)
                 .field(CECommunicationsKeys.accountId)
                 .field(CECommunicationsKeys.emailId)
                 .field(CECommunicationsKeys.type)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("account_enabled_type")
                 .field(CECommunicationsKeys.accountId)
                 .field(CECommunicationsKeys.enabled)
                 .field(CECommunicationsKeys.type)
                 .build())
        .build();
  }

  @Id String uuid;
  @NotBlank String accountId;
  @NotBlank String emailId;
  @NotBlank CommunicationType type;
  boolean enabled;
  boolean selfEnabled;
  long createdAt;
  long lastUpdatedAt;
}
