package software.wings.graphql.schema.type.audit;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
public class QLChangeContent implements QLObject {
  private String changeSetId;
  private String resourceId;
  private String oldYaml;
  private String oldYamlPath;
  private String newYaml;
  private String newYamlPath;
}
