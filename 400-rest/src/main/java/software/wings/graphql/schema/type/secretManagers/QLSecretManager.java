package software.wings.graphql.schema.type.secretManagers;

import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSecretManagerKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLSecretManager implements QLObject {
  String id;
  String name;
  QLUsageScope usageScope;
}
