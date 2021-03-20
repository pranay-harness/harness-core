package software.wings.graphql.schema.type.aggregation.workflow;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(Module._380_CG_GRAPHQL)
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
