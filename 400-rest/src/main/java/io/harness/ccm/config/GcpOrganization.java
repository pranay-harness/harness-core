package io.harness.ccm.config;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
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
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GcpOrganizationKeys")
@Entity(value = "gcpOrganization", noClassnameStored = true)
@TargetModule(Module._490_CE_COMMONS)
public class GcpOrganization implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  String accountId;
  String organizationId;
  String organizationName;
  String serviceAccountEmail;

  long createdAt;
  long lastUpdatedAt;
}
