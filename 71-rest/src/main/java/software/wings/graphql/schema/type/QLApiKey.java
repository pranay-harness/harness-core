package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApiKeyKeys")
@Scope(PermissionAttribute.ResourceType.API_KEY)
public class QLApiKey implements QLObject {
  private String id;
  private String name;
}
