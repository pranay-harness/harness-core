package io.harness.accesscontrol.permissions.persistence;

import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.accesscontrol.permissions.PermissionStatus;
import io.harness.accesscontrol.permissions.validator.PermissionIdentifier;
import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.NGEntityName;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.PersistentEntity;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "PermissionDBOKeys")
@Entity(value = "permissions", noClassnameStored = true)
@Document("permissions")
@TypeAlias("permissions")
@StoreIn(ACCESS_CONTROL)
public class PermissionDBO implements PersistentEntity {
  @Setter @Id @org.mongodb.morphia.annotations.Id String id;
  @FdUniqueIndex @PermissionIdentifier final String identifier;
  @NGEntityName final String name;
  @NotNull final PermissionStatus status;
  @NotEmpty final Set<String> allowedScopeLevels;

  @Setter @CreatedDate Long createdAt;
  @Setter @LastModifiedDate Long lastModifiedAt;
  @Setter @CreatedBy EmbeddedUser createdBy;
  @Setter @LastModifiedBy EmbeddedUser lastUpdatedBy;
  @Setter @Version Long version;
}
