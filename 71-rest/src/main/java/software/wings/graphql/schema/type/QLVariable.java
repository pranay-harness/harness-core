package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLVariableKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLVariable implements QLObject {
  String name;
  String type;
  boolean required;
  List<String> allowedValues;
  String defaultValue;
  boolean fixed;
  String description;
}
