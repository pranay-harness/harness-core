package io.harness.ng.core.environment.beans;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.ng.core.common.beans.Tag;
import io.harness.ng.core.environment.beans.Environment.EnvironmentKeys;
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
@CdUniqueIndex(name = "unique_accountId_organizationIdentifier_projectIdentifier_envIdentifier",
    fields =
    {
      @Field(EnvironmentKeys.accountId)
      , @Field(EnvironmentKeys.orgIdentifier), @Field(EnvironmentKeys.projectIdentifier),
          @Field(EnvironmentKeys.identifier)
    })
@Entity(value = "environmentsNG", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "EnvironmentKeys")
@Document("environmentsNG")
@TypeAlias("io.harness.ng.core.environment.beans.Environment")
public class Environment implements PersistentEntity {
  @Wither @Id @org.mongodb.morphia.annotations.Id private String id;

  @Trimmed @NotEmpty private String accountId;
  @Trimmed @NotEmpty private String orgIdentifier;
  @Trimmed @NotEmpty private String projectIdentifier;

  @NotEmpty @EntityIdentifier private String identifier;
  @EntityName private String name;
  @NotEmpty private EnvironmentType type;
  @NotNull @Singular @Size(max = 128) private List<Tag> tags;

  @Wither @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastModifiedAt;
  @Wither @Version Long version;
}