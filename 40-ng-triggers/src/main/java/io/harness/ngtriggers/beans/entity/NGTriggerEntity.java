package io.harness.ngtriggers.beans.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.target.TargetType;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Size;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NGTriggerEntityKeys")
@NgUniqueIndex(
    name = "unique_accountId_organizationIdentifier_projectIdentifier_targetIdentifier_triggerType_identifier",
    fields =
    {
      @Field(NGTriggerEntity.NGTriggerEntityKeys.accountId)
      , @Field(NGTriggerEntity.NGTriggerEntityKeys.orgIdentifier),
          @Field(NGTriggerEntity.NGTriggerEntityKeys.projectIdentifier),
          @Field(NGTriggerEntity.NGTriggerEntityKeys.targetIdentifier),
          @Field(NGTriggerEntity.NGTriggerEntityKeys.type), @Field(NGTriggerEntity.NGTriggerEntityKeys.identifier)
    })
@Entity(value = "triggersNG", noClassnameStored = true)
@Document("triggersNG")
@TypeAlias("triggersNG")
@HarnessEntity(exportable = true)
public class NGTriggerEntity {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @EntityName String name;
  @EntityIdentifier @NotEmpty String identifier;
  @Size(max = 1024) String description;
  @NotEmpty String yaml;
  @NotEmpty NGTriggerType type;

  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @NotEmpty String projectIdentifier;
  @NotEmpty String targetIdentifier;
  @NotEmpty TargetType targetType;

  @NotEmpty NGTriggerMetadata metadata;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;
  @Builder.Default Boolean deleted = Boolean.FALSE;
}
