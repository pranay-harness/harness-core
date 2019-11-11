package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSettingAttributesKeys")
@Scope(ResourceType.APPLICATION)
public class QLSettingAttribute implements QLObject {
  String id;
  String name;
}
