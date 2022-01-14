package software.wings.beans.template;

public enum TemplateType {
  SSH("SSH"),
  HTTP("HTTP"),
  SERVICE("Service"),
  SERVICE_INFRA("Service Infrastructure"),
  WORKFLOW("Workflow"),
  PIPELINE("Pipeline"),
  SHELL_SCRIPT("Shell Script"),
  ARTIFACT_SOURCE("Artifact Source"),
  PCF_PLUGIN("PCF Command"),
  CUSTOM_DEPLOYMENT_TYPE("Custom Deployment Type");

  String displayName;

  TemplateType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
