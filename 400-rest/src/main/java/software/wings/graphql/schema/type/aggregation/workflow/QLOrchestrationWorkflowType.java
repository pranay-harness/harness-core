package software.wings.graphql.schema.type.aggregation.workflow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public enum QLOrchestrationWorkflowType implements QLEnum {
  BUILD,
  BASIC,
  CANARY,
  MULTI_SERVICE,
  BLUE_GREEN,
  ROLLING,
  CUSTOM;

  @Override
  public String getStringValue() {
    return this.name();
  }
}