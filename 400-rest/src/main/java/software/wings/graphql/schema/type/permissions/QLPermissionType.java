package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
