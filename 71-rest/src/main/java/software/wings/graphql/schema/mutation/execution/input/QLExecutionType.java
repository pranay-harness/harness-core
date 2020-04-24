package software.wings.graphql.schema.mutation.execution.input;

import software.wings.graphql.schema.type.QLEnum;

public enum QLExecutionType implements QLEnum {
  WORKFLOW,
  PIPELINE;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
