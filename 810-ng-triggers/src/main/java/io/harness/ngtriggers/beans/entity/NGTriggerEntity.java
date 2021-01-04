package io.harness.ngtriggers.beans.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.source.NGTriggerType;
import io.harness.ngtriggers.beans.target.TargetType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NGTriggerEntityKeys")
@Entity(value = "triggersNG", noClassnameStored = true)
@Document("triggersNG")
@TypeAlias("triggersNG")
@HarnessEntity(exportable = true)
public class NGTriggerEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(
            CompoundMongoIndex.builder()
                .name(
                    "unique_accountId_organizationIdentifier_projectIdentifier_targetIdentifier_triggerType_identifier")
                .unique(true)
                .field(NGTriggerEntityKeys.accountId)
                .field(NGTriggerEntityKeys.orgIdentifier)
                .field(NGTriggerEntityKeys.projectIdentifier)
                .field(NGTriggerEntityKeys.targetIdentifier)
                .field(NGTriggerEntityKeys.targetType)
                .field(NGTriggerEntityKeys.identifier)
                .build(),
            CompoundMongoIndex.builder()
                .name("unique_accountId_organizationIdentifier_projectIdentifier_identifier")
                .unique(true)
                .field(NGTriggerEntityKeys.accountId)
                .field(NGTriggerEntityKeys.orgIdentifier)
                .field(NGTriggerEntityKeys.projectIdentifier)
                .field(NGTriggerEntityKeys.identifier)
                .build(),
            CompoundMongoIndex.builder()
                .name("type_repoUrl")
                .field(NGTriggerEntityKeys.type)
                .field("metadata.webhook.repoURL")
                .build())
        .build();
  }
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
  @Singular @Size(max = 128) List<NGTag> tags;
  @Builder.Default Boolean enabled = Boolean.TRUE;
}
