package software.wings.graphql.schema.type.permissions;

import software.wings.graphql.schema.type.QLEnum;

public enum QLActions implements QLEnum {
  CREATE,
  READ,
  UPDATE,
  DELETE,
  @Deprecated EXECUTE,
  EXECUTE_WORKFLOW,
  EXECUTE_PIPELINE;
  @Override
  public String getStringValue() {
    return this.name();
  }
}
