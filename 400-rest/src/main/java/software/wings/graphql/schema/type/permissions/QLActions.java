package software.wings.graphql.schema.type.permissions;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLActions implements QLEnum {
  CREATE,
  READ,
  UPDATE,
  DELETE,
  @Deprecated EXECUTE,
  EXECUTE_WORKFLOW,
  EXECUTE_PIPELINE,
  ROLLBACK_WORKFLOW;
  @Override
  public String getStringValue() {
    return this.name();
  }
}
