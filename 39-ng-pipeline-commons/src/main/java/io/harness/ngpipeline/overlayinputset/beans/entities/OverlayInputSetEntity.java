package io.harness.ngpipeline.overlayinputset.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity.OverlayInputSetEntityKeys;
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

import java.util.List;
import javax.validation.constraints.Size;

@Data
@Builder
@FieldNameConstants(innerTypeName = "OverlayInputSetEntityKeys")
@CdUniqueIndex(name = "unique_accountId_organizationIdentifier_projectIdentifier_pipelineIdentifier_inputSetIdentifier",
    fields =
    {
      @Field(OverlayInputSetEntityKeys.accountId)
      , @Field(OverlayInputSetEntityKeys.orgIdentifier), @Field(OverlayInputSetEntityKeys.projectIdentifier),
          @Field(OverlayInputSetEntityKeys.pipelineIdentifier), @Field(OverlayInputSetEntityKeys.identifier)
    })
@CdIndex(name = "accountIdIndex", fields = { @Field(OverlayInputSetEntityKeys.accountId) })
@Entity(value = "overlayInputSetsNG", noClassnameStored = true)
@Document("overlayInputSetsNG")
@TypeAlias("io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity")
@HarnessEntity(exportable = true)
public class OverlayInputSetEntity implements PersistentEntity {
  @Wither @Id @org.mongodb.morphia.annotations.Id private String id;
  @Trimmed @NotEmpty private String accountId;
  @Trimmed @NotEmpty private String orgIdentifier;
  @Trimmed @NotEmpty private String projectIdentifier;
  @Trimmed @NotEmpty private String pipelineIdentifier;

  // OverlayInputSetIdentifier
  @NotEmpty @EntityIdentifier private String identifier;

  @NotEmpty private String overlayInputSetYaml;
  @EntityName private String name;
  @Size(max = 1024) private String description;

  // Add Tags

  List<String> inputSetsReferenceList;

  @Wither @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastModifiedAt;
  @Wither @Version Long version;
  @Builder.Default Boolean deleted = Boolean.FALSE;
}
