package software.wings.graphql.schema.type.cloudProvider;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Scope(ResourceType.SETTING)
public interface QLCloudProvider extends QLObject {
  String getId();
}
