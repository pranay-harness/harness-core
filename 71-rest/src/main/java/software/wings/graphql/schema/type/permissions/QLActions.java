package software.wings.graphql.schema.type.permissions;

import software.wings.graphql.schema.type.QLEnum;

public enum QLActions implements QLEnum {
  CREATE,
  READ,
  UPDATE,
  DELETE,
  EXECUTE;
  @Override
  public String getStringValue() {
    return this.name();
  }
}
