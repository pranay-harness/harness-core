package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.ng.DbAliases.NG_MANAGER;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TerraformRollbackDestroyConfigKeys")
@Entity(value = "terraformRollbackDestroyConfig", noClassnameStored = true)
@Document("terraformRollbackDestroyConfig")
@TypeAlias("terraformRollbackDestroyConfig")
@HarnessEntity(exportable = true)
@StoreIn(NG_MANAGER)
@OwnedBy(CDP)
public class TerraformRollbackDestroyConfig implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("accountId_orgId_projectId_entityId_createdAt")
                 .field(TerraformRollbackDestroyConfigKeys.accountId)
                 .field(TerraformRollbackDestroyConfigKeys.orgId)
                 .field(TerraformRollbackDestroyConfigKeys.projectId)
                 .field(TerraformRollbackDestroyConfigKeys.entityId)
                 .descSortField(TerraformRollbackDestroyConfigKeys.createdAt)
                 .build())
        .build();
  }

  String accountId;
  String orgId;
  String projectId;
  String entityId;
  String pipelineExecutionId;
  long createdAt;

  List<StoreConfig> remoteVarFiles;
  List<String> inlineVarFiles;
  String backendConfig;
  Map<String, String> environmentVariables;
  String workspace;
  List<String> targets;
}
