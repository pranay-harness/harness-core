package io.harness.ngpipeline;

import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.ngpipeline.BaseInputSetEntity.BaseInputSetEntityKeys;
import io.harness.persistence.PersistentEntity;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Size;

@Data
@FieldNameConstants(innerTypeName = "BaseInputSetEntityKeys")
@CdUniqueIndex(name = "unique_accountId_organizationIdentifier_projectIdentifier_pipelineIdentifier_inputSetIdentifier",
    fields =
    {
      @Field(BaseInputSetEntityKeys.accountId)
      , @Field(BaseInputSetEntityKeys.orgIdentifier), @Field(BaseInputSetEntityKeys.projectIdentifier),
          @Field(BaseInputSetEntityKeys.pipelineIdentifier), @Field(BaseInputSetEntityKeys.identifier)
    })
@CdIndex(name = "accountIdIndex", fields = { @Field(BaseInputSetEntityKeys.accountId) })
@Entity(value = "inputSetsNG", noClassnameStored = true)
@Document("inputSetsNG")
@HarnessEntity(exportable = true)
public abstract class BaseInputSetEntity implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id private String id;
  @Trimmed @NotEmpty String accountId;
  @Trimmed @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @Trimmed @NotEmpty String pipelineIdentifier;

  @NotEmpty @EntityIdentifier String identifier; // input set identifier
  @EntityName String name;
  @Size(max = 1024) String description;

  // Add Tags

  @NotEmpty String inputSetYaml;
  @NotEmpty InputSetEntityType inputSetType;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  Boolean deleted = Boolean.FALSE;
}
