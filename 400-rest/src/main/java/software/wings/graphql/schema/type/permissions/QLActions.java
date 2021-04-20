package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
