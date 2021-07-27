package software.wings.graphql.schema.type.aggregation.service;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(HarnessTeam.DX)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLWorkflowType implements QLEnum {
  PIPELINE,
  ORCHESTRATION;

  @Override
  public String getStringValue() {
    return this.name();
  }
}