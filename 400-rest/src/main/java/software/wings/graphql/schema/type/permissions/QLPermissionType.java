package software.wings.graphql.schema.type.permissions;

import software.wings.graphql.schema.type.QLEnum;

public enum QLPermissionType implements QLEnum {
  ALL,
  ENV,
  SERVICE,
  WORKFLOW,
  PIPELINE,
  DEPLOYMENT,
  PROVISIONER;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
