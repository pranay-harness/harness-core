package io.harness.accesscontrol.permissions.database;

import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.accesscontrol.scopes.Scope;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.PersistentEntity;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PermissionKeys")
@Document("permissions")
@TypeAlias("permissions")
class Permission implements PersistentEntity {
  @Id String id;
  @EntityIdentifier @FdUniqueIndex String identifier;
  @NGEntityName String displayName;
  @NotEmpty String resourceType;
  @NotEmpty String action;
  @NotNull PermissionStatus status;
  @NotEmpty Set<Scope> scopes;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @CreatedBy EmbeddedUser createdBy;
  @LastModifiedBy EmbeddedUser lastUpdatedBy;
  @Version long version;
}
