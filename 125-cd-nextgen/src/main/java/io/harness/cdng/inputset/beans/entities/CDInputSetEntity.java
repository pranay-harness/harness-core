package io.harness.cdng.inputset.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity.CDInputSetEntityKeys;
import io.harness.cdng.inputset.beans.yaml.CDInputSet;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Data;
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

import javax.validation.constraints.Size;

@Data
@Builder
@FieldNameConstants(innerTypeName = "CDInputSetEntityKeys")
@CdUniqueIndex(name = "unique_accountId_organizationIdentifier_projectIdentifier_pipelineIdentifier_inputSetIdentifier",
    fields =
    {
      @Field(CDInputSetEntityKeys.accountId)
      , @Field(CDInputSetEntityKeys.orgIdentifier), @Field(CDInputSetEntityKeys.projectIdentifier),
          @Field(CDInputSetEntityKeys.pipelineIdentifier), @Field(CDInputSetEntityKeys.identifier)
    })
@CdIndex(name = "accountIdIndex", fields = { @Field(CDInputSetEntityKeys.accountId) })
@Entity(value = "inputSetsNG", noClassnameStored = true)
@Document("inputSetsNG")
@TypeAlias("io.harness.cdng.inputset.beans.entities.CDInputSetEntity")
@HarnessEntity(exportable = true)
public class CDInputSetEntity implements PersistentEntity {
  @Wither @Id @org.mongodb.morphia.annotations.Id private String id;
  @Trimmed @NotEmpty private String accountId;
  @NotEmpty @EntityIdentifier private String identifier;
  @Trimmed @NotEmpty private String orgIdentifier;
  @Trimmed @NotEmpty private String projectIdentifier;
  @Trimmed @NotEmpty private String pipelineIdentifier;

  @EntityName private String name;

  private CDInputSet cdInputSet;
  @NotEmpty private String inputSetYaml;
  @Size(max = 1024) private String description;

  // Add Tags

  @Wither @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastModifiedAt;
  @Wither @Version Long version;
  @Builder.Default Boolean deleted = Boolean.FALSE;
}
