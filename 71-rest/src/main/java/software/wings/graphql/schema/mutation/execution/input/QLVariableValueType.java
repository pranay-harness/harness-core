package software.wings.graphql.schema.mutation.execution.input;

import software.wings.graphql.schema.type.QLEnum;

public enum QLVariableValueType implements QLEnum {
  ID,
  NAME;

  @Override
  public String getStringValue() {
    return this.name();
  }
}