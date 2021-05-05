package io.harness.ngpipeline.pipeline.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "PipelineNGKeys")
@Entity(value = "pipelinesNG", noClassnameStored = true)
@Document("pipelinesNG")
@TypeAlias("pipelinesNG")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.NG_MANAGER)
public class NgPipelineEntity implements PersistentEntity, AccountAccess, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  private NgPipeline ngPipeline;
  @NotEmpty String yamlPipeline;
  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @NotEmpty String identifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @SchemaIgnore @FdIndex @CreatedDate private long createdAt;
  @SchemaIgnore @NotNull @LastModifiedDate private long lastUpdatedAt;
  @Builder.Default Boolean deleted = Boolean.FALSE;

  @EntityName String name;
  @Size(max = 1024) String description;
  @Singular @Size(max = 128) List<NGTag> tags;

  @Version Long version;

  Set<EntityDetail> referredEntities;

  @Override
  public String getAccountId() {
    return accountId;
  }
}
