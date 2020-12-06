package software.wings.graphql.schema.mutation.execution.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Data
@Builder
@FieldNameConstants(innerTypeName = "QLVariableInputKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
@NoArgsConstructor
@AllArgsConstructor
public class QLVariableInput {
  private String name;
  private QLVariableValue variableValue;
}
