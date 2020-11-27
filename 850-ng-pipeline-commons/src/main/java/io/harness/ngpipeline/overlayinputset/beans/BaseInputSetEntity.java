package io.harness.ngpipeline.overlayinputset.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity.BaseInputSetEntityKeys;
import io.harness.persistence.PersistentEntity;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@FieldNameConstants(innerTypeName = "BaseInputSetEntityKeys")
@NgUniqueIndex(name = "unique_accountId_organizationIdentifier_projectIdentifier_pipelineIdentifier_inputSetIdentifier",
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

  @Singular @Size(max = 128) List<NGTag> tags;

  @NotEmpty String inputSetYaml;
  @NotEmpty InputSetEntityType inputSetType;

  Set<EntityDetail> referredEntities;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  Boolean deleted = Boolean.FALSE;
}
