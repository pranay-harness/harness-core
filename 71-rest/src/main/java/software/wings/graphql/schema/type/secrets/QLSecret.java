package software.wings.graphql.schema.type.secrets;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Scope(PermissionAttribute.ResourceType.SETTING)
public interface QLSecret extends QLObject {
  String getId();
  QLSecretType getSecretType();
  String getName();
}
