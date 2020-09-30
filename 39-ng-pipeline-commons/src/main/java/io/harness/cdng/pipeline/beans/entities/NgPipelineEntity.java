package io.harness.cdng.pipeline.beans.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "PipelineNGKeys")
@NgUniqueIndex(name = "unique_accountIdentifier_organizationIdentifier_projectIdentifier_pipelineIdentifier",
    fields =
    {
      @Field(NgPipelineEntity.PipelineNGKeys.accountId)
      , @Field(NgPipelineEntity.PipelineNGKeys.orgIdentifier),
          @Field(NgPipelineEntity.PipelineNGKeys.projectIdentifier), @Field(NgPipelineEntity.PipelineNGKeys.identifier)
    })
@CdIndex(name = "accountIdentifierIndex", fields = { @Field(NgPipelineEntity.PipelineNGKeys.accountId) })
@Entity(value = "pipelinesNG", noClassnameStored = true)
@Document("pipelinesNG")
@TypeAlias("pipelinesNG")
@HarnessEntity(exportable = true)
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

  // add name, description, and tags

  @Override
  public String getAccountId() {
    return accountId;
  }
}
