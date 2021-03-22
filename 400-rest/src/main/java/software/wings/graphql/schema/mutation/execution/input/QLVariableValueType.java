package software.wings.graphql.schema.mutation.execution.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLVariableValueType implements QLEnum {
  ID,
  NAME,
  EXPRESSION;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
