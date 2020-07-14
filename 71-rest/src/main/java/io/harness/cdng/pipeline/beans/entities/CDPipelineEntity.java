package io.harness.cdng.pipeline.beans.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
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
@CdUniqueIndex(name = "unique_accountIdentifier_organizationIdentifier_projectIdentifier_pipelineIdentifier",
    fields =
    {
      @Field(CDPipelineEntity.PipelineNGKeys.accountId)
      , @Field(CDPipelineEntity.PipelineNGKeys.orgIdentifier),
          @Field(CDPipelineEntity.PipelineNGKeys.projectIdentifier), @Field(CDPipelineEntity.PipelineNGKeys.identifier)
    })
@CdIndex(name = "accountIdentifierIndex", fields = { @Field(CDPipelineEntity.PipelineNGKeys.accountId) })
@Entity(value = "pipelinesNG", noClassnameStored = true)
@Document("pipelinesNG")
@TypeAlias("pipelinesNG")
@HarnessEntity(exportable = true)
public class CDPipelineEntity implements PersistentEntity, AccountAccess, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  private CDPipeline cdPipeline;
  @NotEmpty String yamlPipeline;
  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @NotEmpty String identifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @SchemaIgnore @FdIndex @CreatedDate private long createdAt;
  @SchemaIgnore @NotNull @LastModifiedDate private long lastUpdatedAt;

  @Override
  public String getAccountId() {
    return accountId;
  }
}
