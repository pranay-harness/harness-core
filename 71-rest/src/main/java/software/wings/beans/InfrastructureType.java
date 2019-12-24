package software.wings.beans;

import lombok.experimental.UtilityClass;

/*
** Class for constants defining Infrastructure types
*/
@UtilityClass
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
}
