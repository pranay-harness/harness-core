package software.wings.stencils;

public enum WorkflowStepType {
  /*
    Important : This enum defines the Command Categories that appear in UI. Please do not change the order of entries
    here. The command categories listed in Add Command step in UI are governed by the order of entries in this enum.
   */

  ARTIFACT("Artifact"),

  // ssh categories
  AWS_SSH("AWS"),
  DC_SSH("Data Center"),
  AZURE("Azure"),

  // AZURE VMSS
  AZURE_VMSS("Azure Virtual Machine Scale Set"),
  AZURE_WEBAPP("Azure Web App"),

  // AWS
  AWS_CODE_DEPLOY("AWS Code Deploy"),
  AWS_LAMBDA("AWS Lambda"),
  AWS_AMI("AMI"),
  ECS("ECS"),

  // Spotinst
  SPOTINST("Spot Instance"),

  // K8s
  KUBERNETES("Kubernetes"),

  // Helm
  HELM("Helm"),
  // PCF
  PCF("Tanzu Application Services"),

  // Infrastructure Provisioner,
  INFRASTRUCTURE_PROVISIONER("Infrastructure Provisioner"),

  // verification providers
  APM("Performance Monitoring"),
  LOG("Log Analysis"),

  CVNG("Harness CV 2.0 - All Monitoring Sources"),

  ISSUE_TRACKING("Issue Tracking"),
  NOTIFICATION("Notification"),
  FLOW_CONTROL("Flow Control"),
  CI_SYSTEM("CI System"),
  UTILITY("Utility"),
  SERVICE_COMMAND("Service Command"),

  // ARGO
  ARGO("Argo");

  String displayName;

  WorkflowStepType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
