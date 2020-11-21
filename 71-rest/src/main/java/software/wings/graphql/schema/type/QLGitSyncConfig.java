package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLGitSyncConfigKeys")
@Scope(ResourceType.APPLICATION)
public class QLGitSyncConfig implements QLObject {
  String gitConnectorId;
  private String repositoryName;
  String branch;
  Boolean syncEnabled;
}
