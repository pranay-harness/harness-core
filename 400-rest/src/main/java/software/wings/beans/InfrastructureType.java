package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.experimental.UtilityClass;

/*
** Class for constants defining Infrastructure types
*/
@UtilityClass
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
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
