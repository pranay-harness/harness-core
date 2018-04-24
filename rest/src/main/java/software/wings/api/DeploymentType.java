package software.wings.api;

/**
 * Created by rishi on 12/22/16.
 */
public enum DeploymentType {
  SSH("Secure Shell (SSH)"),
  AWS_CODEDEPLOY("AWS CodeDeploy"),
  ECS("Amazon EC2 Container Services (ECS)"),
  KUBERNETES("Kubernetes"),
  HELM("Helm"),
  AWS_LAMBDA("AWS Lambda"),
  AMI("AMI"),
  WINRM("Windows Remote Management (WinRM)"),
  PCF("Pivotal Cloud Foundry");

  private String displayName;

  DeploymentType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
