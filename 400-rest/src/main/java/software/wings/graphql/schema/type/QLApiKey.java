package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApiKeyKeys")
@Scope(PermissionAttribute.ResourceType.API_KEY)
public class QLApiKey implements QLObject {
  private String id;
  private String name;
}
