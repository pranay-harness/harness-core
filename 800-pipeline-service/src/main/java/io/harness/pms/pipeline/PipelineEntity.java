package io.harness.pms.pipeline;

import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import com.sun.istack.internal.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Setter;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "PipelineEntityKeys")
@Entity(value = "pipelinesPMS", noClassnameStored = true)
@Document("pipelinesPMS")
@TypeAlias("pipelinesPMS")
@HarnessEntity(exportable = true)
public class PipelineEntity implements PersistentEntity, AccountAccess, UuidAware, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_organizationId_projectId_pipelineId")
                 .unique(true)
                 .field(PipelineEntityKeys.accountId)
                 .field(PipelineEntityKeys.orgIdentifier)
                 .field(PipelineEntityKeys.projectIdentifier)
                 .field(PipelineEntityKeys.identifier)
                 .build())
        .build();
  }

  @Setter @NonFinal @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotEmpty String yaml;
  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @NotEmpty String identifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @Setter @NonFinal int stageCount;
  @Setter @NonFinal @SchemaIgnore @FdIndex @CreatedDate long createdAt;
  @Setter @NonFinal @SchemaIgnore @NotNull @LastModifiedDate long lastUpdatedAt;
  @Default Boolean deleted = Boolean.FALSE;
  List<String> branches;
  String objectId;

  @EntityName String name;
  @Size(max = 1024) String description;
  @Singular @Size(max = 128) List<NGTag> tags;

  @Setter @NonFinal @Version Long version;
  @Default Map<String, org.bson.Document> filters = new HashMap<>();
  ExecutionSummaryInfo executionSummaryInfo;
  int runSequence;
  @Setter @NonFinal @Singular List<String> stageNames;

  @Override
  public String getAccountId() {
    return accountId;
  }
}
