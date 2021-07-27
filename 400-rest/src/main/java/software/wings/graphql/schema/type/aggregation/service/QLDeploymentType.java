package software.wings.graphql.schema.type.aggregation.service;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public enum QLDeploymentType implements QLEnum {
  SSH,
  AWS_CODEDEPLOY,
  ECS,
  SPOTINST,
  KUBERNETES,
  HELM,
  AWS_LAMBDA,
  AMI,
  WINRM,
  PCF,
  AZURE_VMSS,
  AZURE_WEBAPP,
  CUSTOM;

  @Override
  public String getStringValue() {
    return this.name();
  }
}