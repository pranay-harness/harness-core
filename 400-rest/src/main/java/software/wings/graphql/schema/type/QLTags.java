package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTagsKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLTags {
  String name;
  String[] values;
}
