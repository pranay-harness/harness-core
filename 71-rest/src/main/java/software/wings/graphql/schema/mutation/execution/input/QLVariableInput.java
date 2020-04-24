package software.wings.graphql.schema.mutation.execution.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLVariableInputKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLVariableInput {
  String name;
  QLVariableValue variableValue;
}
