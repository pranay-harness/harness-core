/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

/*
** Class for constants defining Infrastructure types
*/
@UtilityClass
@OwnedBy(CDP)
public class InfrastructureType {
  public static final String GCP_KUBERNETES_ENGINE = "GCP_KUBERNETES";
  public static final String PHYSICAL_INFRA = "PHYSICAL_DATA_CENTER_SSH";
  public static final String PHYSICAL_INFRA_WINRM = "PHYSICAL_DATA_CENTER_WINRM";
  public static final String PCF_INFRASTRUCTURE = "PCF_PCF";
  public static final String DIRECT_KUBERNETES = "DIRECT_KUBERNETES";
  public static final String CODE_DEPLOY = "AWS_AWS_CODEDEPLOY";
  public static final String AWS_AMI = "AWS_AMI";
  public static final String AWS_AMI_LT = "AWS_AMI_LT";
  public static final String AWS_ECS = "AWS_ECS";
  public static final String AWS_INSTANCE = "AWS_SSH";
  public static final String AWS_LAMBDA = "AWS_AWS_LAMBDA";
  public static final String AZURE_SSH = "AZURE_INFRA";
  public static final String AZURE_KUBERNETES = "AZURE_KUBERNETES";
  public static final String AZURE_VMSS = "AZURE_VMSS";
  public static final String AZURE_WEBAPP = "AZURE_WEBAPP";
  public static final String PDC = "PDC";
  public static final String CUSTOM_INFRASTRUCTURE = "CUSTOM";
}
