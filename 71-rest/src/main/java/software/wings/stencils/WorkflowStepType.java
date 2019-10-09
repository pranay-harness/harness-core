package software.wings.stencils;

public enum WorkflowStepType {
  /*
    Important : This enum defines the Command Categories that appear in UI. Please do not change the order of entries
    here. The command categories listed in Add Command step in UI are governed by the order of entries in this enum.
   */

  ARTIFACT("Artifact"),

  APM("Performance Monitoring"),
  LOG("Log Analysis"),

  // ssh categories
  AWS_SSH("AWS SSH"),
  DC_SSH("DC SSH"),
  AZURE("Azure"),

  // AWS
  AWS_CODE_DEPLOY("AWS Code Deploy"),
  AWS_LAMBDA("AWS Lambda"),
  AWS_AMI("AMI"),
  ECS("ECS"),

  // Spotinst
  SPOTINST("Spot Instance"),

  // K8s
  KUBERNETES("Kubernetes"),

  HELM("Helm"),
  PCF("Pivotal Cloud Foundry"),
  CLOUDFORMATION("Cloud Formation"),

  // Approval,
  INFRASTRUCTURE_PROVISIONER("Infrastructure Provisioner"),
  ISSUE_TRACKING("Issue Tracking"),
  NOTIFICATION("Notification"),
  FLOW_CONTROL("Flow Control"),
  CI_SYSTEM("CI System"),
  UTILITY("Utility"),
  SERVICE_COMMAND("Service Command");

  String displayName;

  WorkflowStepType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
