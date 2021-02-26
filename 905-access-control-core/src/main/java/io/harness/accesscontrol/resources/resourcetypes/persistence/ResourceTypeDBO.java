package io.harness.accesscontrol.resources.resourcetypes.persistence;

import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.FdUniqueIndex;

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
@FieldNameConstants(innerTypeName = "ResourceTypeDBOKeys")
@Entity(value = "resourcetypes", noClassnameStored = true)
@Document("resourcetypes")
@TypeAlias("resourcetypes")
@StoreIn(ACCESS_CONTROL)
public class ResourceTypeDBO {
  @Setter @Id @org.mongodb.morphia.annotations.Id String id;
  @FdUniqueIndex @NotEmpty final String identifier;
  @FdUniqueIndex @NotEmpty final String permissionKey;

  @Setter @CreatedDate Long createdAt;
  @Setter @LastModifiedDate Long lastModifiedAt;
  @Setter @CreatedBy EmbeddedUser createdBy;
  @Setter @LastModifiedBy EmbeddedUser lastUpdatedBy;
  @Setter @Version Long version;
}
