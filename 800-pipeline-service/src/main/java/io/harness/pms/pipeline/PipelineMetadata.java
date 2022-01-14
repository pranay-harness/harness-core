package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.gitsync.persistance.GitSyncableEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "PipelineMetadataKeys")
@Entity(value = "pipelineMetadata", noClassnameStored = true)
@Document("pipelineMetadata")
@TypeAlias("pipelineMetadata")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.PMS)
public class PipelineMetadata implements GitSyncableEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("account_org_project_pipeline_yaml_git_config_branch_idx")
                 .unique(true)
                 .field(PipelineMetadataKeys.accountIdentifier)
                 .field(PipelineMetadataKeys.orgIdentifier)
                 .field(PipelineMetadataKeys.projectIdentifier)
                 .field(PipelineMetadataKeys.identifier)
                 .field(PipelineMetadataKeys.yamlGitConfigRef)
                 .field(PipelineMetadataKeys.branch)
                 .build())
        .build();
  }

  @Setter @NonFinal @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotEmpty String accountIdentifier;
  @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @NotEmpty String identifier;

  @Wither @Setter @NonFinal String objectIdOfYaml;
  @Setter @NonFinal Boolean isFromDefaultBranch;
  @Setter @NonFinal String branch;
  @Setter @NonFinal String yamlGitConfigRef;
  @Setter @NonFinal String filePath;
  @Setter @NonFinal String rootFolder;
  @Getter(AccessLevel.NONE) @Wither @NonFinal Boolean isEntityInvalid;

  ExecutionSummaryInfo executionSummaryInfo;
  int runSequence;

  @Override
  public boolean isEntityInvalid() {
    return Boolean.TRUE.equals(isEntityInvalid);
  }

  @Override
  public void setEntityInvalid(boolean isEntityInvalid) {
    this.isEntityInvalid = isEntityInvalid;
  }

  @Override
  public String getInvalidYamlString() {
    return "";
  }
}
