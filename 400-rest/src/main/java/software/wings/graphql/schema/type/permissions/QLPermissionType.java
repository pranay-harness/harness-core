package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.PL)
public enum QLPermissionType implements QLEnum {
  ALL,
  ENV,
  SERVICE,
  WORKFLOW,
  PIPELINE,
  DEPLOYMENT,
  PROVISIONER,
  TEMPLATE;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
