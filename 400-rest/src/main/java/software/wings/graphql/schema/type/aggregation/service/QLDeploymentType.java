/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.aggregation.service;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
