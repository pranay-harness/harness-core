package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSettingAttributesKeys")
@Scope(ResourceType.APPLICATION)
public class QLSettingAttribute implements QLObject {
  String id;
  String name;
}
