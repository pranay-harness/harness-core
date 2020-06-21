package io.harness.ng.core.entities;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.UniqueIndex;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ProjectKeys")
@UniqueIndex(name = "unique_orgIdentifier_projectIdentifier",
    fields = { @Field(ProjectKeys.orgId)
               , @Field(ProjectKeys.identifier) })
@Entity(value = "projects", noClassnameStored = true)
@Document("projects")
@TypeAlias("projects")
public class Project implements PersistentEntity, AccountAccess {
  @Wither @Id @org.mongodb.morphia.annotations.Id String id;
  @Trimmed @NotEmpty String accountId;
  @Trimmed @NotEmpty String orgId;
  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty @EntityName String name;
  @Trimmed @NotEmpty String color;
  @NotNull @Size(max = 1024) String purpose;
  @NotNull @Size(max = 1024) String description;
  @Singular @Size(min = 1, max = 128) List<String> owners;
  @NotNull @Singular @Size(max = 128) List<String> tags;
  @Wither @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastModifiedAt;
  @Wither @Version Long version;
  @Builder.Default Boolean deleted = Boolean.FALSE;
}
