package io.harness.ccm.config;

import io.harness.ccm.config.GcpServiceAccount.GcpServiceAccountKeys;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GcpServiceAccountKeys")
@Entity(value = "gcpServiceAccount", noClassnameStored = true)
@NgUniqueIndex(name = "no_dup",
    fields = { @Field(GcpServiceAccountKeys.accountId)
               , @Field(GcpServiceAccountKeys.serviceAccountId) })
public class GcpServiceAccount implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  @NotEmpty String serviceAccountId;
  @NotEmpty String accountId;
  @NotEmpty String gcpUniqueId;
  @NotEmpty String email;
  long createdAt;
  long lastUpdatedAt;

  @Builder
  public GcpServiceAccount(String serviceAccountId, String gcpUniqueId, String accountId, String email) {
    this.serviceAccountId = serviceAccountId;
    this.gcpUniqueId = gcpUniqueId;
    this.accountId = accountId;
    this.email = email;
  }
}
