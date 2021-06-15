package io.harness.ng.serviceaccounts.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.NGOrgAccess;
import io.harness.ng.core.NGProjectAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ServiceAccountKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "serviceAccounts", noClassnameStored = true)
@Document("serviceAccounts")
@TypeAlias("serviceAccounts")
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.PL)
public class ServiceAccount implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, NGAccountAccess,
                                       NGOrgAccess, NGProjectAccess {
  @Id String uuid;
  long createdAt;
  long lastUpdatedAt;

  String identifier;
  String name;
  @NotNull @Size(max = 1024) String description;

  @NotNull String accountIdentifier;
  @EntityIdentifier(allowBlank = true) String orgIdentifier;
  @EntityIdentifier(allowBlank = true) String projectIdentifier;
}
