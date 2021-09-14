/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
