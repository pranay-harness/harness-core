package io.harness.ccm.communication.entities;

import io.harness.mongo.index.Field;
import io.harness.mongo.index.UniqueIndex;
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

@UniqueIndex(name = "account_email_type", fields = { @Field("accountId")
                                                     , @Field("emailId"), @Field("type") })
@UniqueIndex(name = "account_enabled_type", fields = { @Field("accountId")
                                                       , @Field("enabled"), @Field("type") })
@Data
@Builder
@FieldNameConstants(innerTypeName = "CECommunicationsKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceCommunications", noClassnameStored = true)
public class CECommunications implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  @NotBlank String accountId;
  @NotBlank String emailId;
  @NotBlank CommunicationType type;
  boolean enabled;
  long createdAt;
  long lastUpdatedAt;
}
