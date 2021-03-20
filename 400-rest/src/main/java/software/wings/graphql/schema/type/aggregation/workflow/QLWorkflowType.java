package software.wings.graphql.schema.type.aggregation.workflow;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(Module._380_CG_GRAPHQL)
public enum QLWorkflowType implements QLEnum {
  PIPELINE,
  ORCHESTRATION;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
