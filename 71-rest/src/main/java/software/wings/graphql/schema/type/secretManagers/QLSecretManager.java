package software.wings.graphql.schema.type.secretManagers;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSecretManagerKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLSecretManager implements QLObject {
  String id;
  String name;
}
