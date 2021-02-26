package io.harness.ng.core.entities;

import io.harness.ModuleType;
import io.harness.annotation.StoreIn;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.mongo.CollationLocale;
import io.harness.mongo.CollationStrength;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.RsqlQueryable;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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

@Data
@Builder
@FieldNameConstants(innerTypeName = "ProjectKeys")
@RsqlQueryable(fields = { @Field(ProjectKeys.modules)
                          , @Field(ProjectKeys.orgIdentifier) })
@Entity(value = "projects", noClassnameStored = true)
@Document("projects")
@TypeAlias("projects")
@StoreIn(DbAliases.NG_MANAGER)
public class Project implements PersistentEntity, NGAccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountIdentifier_organizationIdentifier_projectIdentifier")
                 .field(ProjectKeys.accountIdentifier)
                 .field(ProjectKeys.orgIdentifier)
                 .field(ProjectKeys.identifier)
                 .unique(true)
                 .collation(CompoundMongoIndex.Collation.builder()
                                .locale(CollationLocale.ENGLISH)
                                .strength(CollationStrength.PRIMARY)
                                .build())
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("acctModulesOrgIdx")
                 .field(ProjectKeys.accountIdentifier)
                 .field(ProjectKeys.modules)
                 .field(ProjectKeys.orgIdentifier)
                 .unique(false)
                 .build())
        .build();
  }

  @Wither @Id @org.mongodb.morphia.annotations.Id String id;
  String accountIdentifier;
  @EntityIdentifier(allowBlank = false) String identifier;
  @EntityIdentifier(allowBlank = false) String orgIdentifier;

  @NGEntityName String name;
  @NotEmpty String color;
  @NotNull @Singular @Size(max = 1024) List<ModuleType> modules;
  @NotNull @Size(max = 1024) String description;
  @NotNull @Singular @Size(max = 128) List<NGTag> tags;

  @Wither @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastModifiedAt;
  @Wither @Version Long version;
  @Builder.Default Boolean deleted = Boolean.FALSE;
}
