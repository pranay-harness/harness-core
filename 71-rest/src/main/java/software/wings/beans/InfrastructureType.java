package software.wings.beans;

import lombok.experimental.UtilityClass;

/*
** Class for constants defining Infrastructure types
*/
@UtilityClass
public class InfrastructureType {
  public static final String GCP_KUBERNETES_ENGINE = "GCP_KUBERNETES_ENGINE";
  public static final String PHYSICAL_INFRA = "PHYSICAL_INFRA";
  public static final String PHYSICAL_INFRA_WINRM = "PHYSICAL_INFRA_WINRM";
  public static final String PCF_INFRASTRUCTURE = "PCF_INFRASTRUCTURE";
  public static final String DIRECT_KUBERNETES = "DIRECT_KUBERNETES";
  public static final String CODE_DEPLOY = "CODE_DEPLOY";
  public static final String AWS_AMI = "AWS_AMI";
  public static final String AWS_ECS = "AWS_ECS";
  public static final String AWS_INSTANCE = "AWS_INSTANCE";
  public static final String AWS_LAMBDA = "AWS_LAMBDA";
  public static final String AZURE_SSH = "AZURE_SSH";
  public static final String AZURE_KUBERNETES = "AZURE_KUBERNETES";
}
