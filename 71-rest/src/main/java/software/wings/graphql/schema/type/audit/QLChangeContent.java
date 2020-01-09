package software.wings.graphql.schema.type.audit;

import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
public class QLChangeContent {
  private String changeSetId;
  private String resourceId;
  private String oldYaml;
  private String oldYamlPath;
  private String newYaml;
  private String newYamlPath;
}
