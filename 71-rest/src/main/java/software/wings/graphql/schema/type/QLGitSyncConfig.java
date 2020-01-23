package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLGitSyncConfigKeys")
@Scope(ResourceType.APPLICATION)
public class QLGitSyncConfig implements QLObject {
  String gitConnectorId;
  String branch;
  Boolean syncEnabled;
}
