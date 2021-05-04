package io.harness.cdng.provision.terraform;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.GitStoreConfigDTO;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TerraformConfigKeys")
@Entity(value = "terraformConfig", noClassnameStored = true)
@Document("terraformConfig")
@TypeAlias("terraformConfig")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.NG_MANAGER)
@OwnedBy(HarnessTeam.CDP)
public class TerraformConfig implements PersistentEntity, CreatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("accountId_orgId_projectId_entityId_createdAt")
                 .field(TerraformConfigKeys.accountId)
                 .field(TerraformConfigKeys.orgId)
                 .field(TerraformConfigKeys.projectId)
                 .field(TerraformConfigKeys.entityId)
                 .descSortField(TerraformConfigKeys.createdAt)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id private String uuid;
  String accountId;
  String orgId;
  String projectId;
  String entityId;
  String pipelineExecutionId;
  long createdAt;

  GitStoreConfigDTO configFiles;
  List<GitStoreConfigDTO> remoteVarFiles;
  List<String> inlineVarFiles;
  String backendConfig;
  Map<String, String> environmentVariables;
  String workspace;
  List<String> targets;
}
