package software.wings.graphql.schema.type;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLGitSyncConfigKeys")
@Scope(ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLGitSyncConfig implements QLObject {
  String gitConnectorId;
  private String repositoryName;
  String branch;
  Boolean syncEnabled;
}
