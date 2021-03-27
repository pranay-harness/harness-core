package io.harness.cdng.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.executions.steps.StepSpecTypeConstants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

/*
   Todo: Change StepSpecTypeConstants.PLACEHOLDER to their respective type once the StepInfo for those is implemented.
 */
@OwnedBy(CDP)
public enum NGStepType {
  // k8s steps
  @JsonProperty(StepSpecTypeConstants.TERRAFORM_APPLY)
  APPLY("Apply", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecTypeConstants.PLACEHOLDER),
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  SCALE("Scale", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecTypeConstants.PLACEHOLDER),
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  STAGE_DEPLOYMENT("Stage Deployment", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes",
      StepSpecTypeConstants.PLACEHOLDER),
  @JsonProperty(StepSpecTypeConstants.K8S_ROLLING_DEPLOY)
  K8S_ROLLING("K8s Rolling", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes",
      StepSpecTypeConstants.K8S_ROLLING_DEPLOY),
  @JsonProperty(StepSpecTypeConstants.K8S_ROLLING_ROLLBACK)
  K8S_ROLLING_ROLLBACK("K8s Rolling Rollback", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes",
      StepSpecTypeConstants.K8S_ROLLING_ROLLBACK),

  @JsonProperty(StepSpecTypeConstants.K8S_BG_SWAP_SERVICES)
  SWAP_SELECTORS("Swap Selectors", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes",
      StepSpecTypeConstants.K8S_BG_SWAP_SERVICES),
  @JsonProperty(StepSpecTypeConstants.K8S_DELETE)
  DELETE("Delete", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecTypeConstants.K8S_DELETE),
  @JsonProperty(StepSpecTypeConstants.K8S_CANARY_DELETE)
  K8S_CANARY_DELETE("Canary Delete", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes",
      StepSpecTypeConstants.K8S_CANARY_DELETE),
  @JsonProperty(StepSpecTypeConstants.K8S_CANARY_DEPLOY)
  CANARY_DEPLOYMENT("Deployment", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes",
      StepSpecTypeConstants.K8S_CANARY_DEPLOY),

  // Infrastructure Provisioners
  @JsonProperty(StepSpecTypeConstants.TERRAFORM_APPLY)
  TERRAFORM_APPLY("Terraform Apply", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Terraform", StepSpecTypeConstants.TERRAFORM_APPLY),
  @JsonProperty(StepSpecTypeConstants.TERRAFORM_PLAN)
  TERRAFORM_PLAN("Terraform Plan", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Terraform", StepSpecTypeConstants.TERRAFORM_PLAN),
  @JsonProperty(StepSpecTypeConstants.TERRAFORM_DESTROY)
  TERRAFORM_DESTROY("Terraform Destroy", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Terraform", StepSpecTypeConstants.TERRAFORM_DESTROY),
  @JsonProperty(StepSpecTypeConstants.TERRAFORM_ROLLBACK)
  TERRAFORM_ROLLBACK("Terraform Rollback", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Terraform", StepSpecTypeConstants.TERRAFORM_ROLLBACK),
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  CREATE_STACK("Create Stack", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Cloudformation", StepSpecTypeConstants.PLACEHOLDER),
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  DELETE_STACK("Delete Stack", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Cloudformation", StepSpecTypeConstants.PLACEHOLDER),
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  SHELL_SCRIPT_PROVISIONER("Shell Script Provisioner", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Shell Script Provisioner", StepSpecTypeConstants.PLACEHOLDER),

  // Issue Tracking
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  JIRA("Jira", Arrays.asList(ServiceDefinitionType.values()), "Issue Tracking", StepSpecTypeConstants.PLACEHOLDER),
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  SERVICENOW(
      "ServiceNow", Arrays.asList(ServiceDefinitionType.values()), "Issue Tracking", StepSpecTypeConstants.PLACEHOLDER),
  // Notifications
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  EMAIL("Email", Arrays.asList(ServiceDefinitionType.values()), "Notification", StepSpecTypeConstants.PLACEHOLDER),
  // Flow Control
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  BARRIERS(
      "Barriers", Arrays.asList(ServiceDefinitionType.values()), "Flow Control", StepSpecTypeConstants.PLACEHOLDER),
  // Utilities
  @JsonProperty(StepSpecTypeConstants.SHELL_SCRIPT)
  SHELL_SCRIPT("Shell Script", Arrays.asList(ServiceDefinitionType.values()), "Utilites/Scripted",
      StepSpecTypeConstants.SHELL_SCRIPT),
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  NEW_RELIC_DEPLOYMENT_MAKER("New Relic Deployment Maker", Arrays.asList(ServiceDefinitionType.values()),
      "Utilites/Non-Scripted/", StepSpecTypeConstants.PLACEHOLDER),
  @JsonProperty(StepSpecTypeConstants.PLACEHOLDER)
  TEMPLATIZED_SECRET_MANAGER("Templatized Secret Manager", Arrays.asList(ServiceDefinitionType.values()),
      "Utilites/Non-Scripted/", StepSpecTypeConstants.PLACEHOLDER);

  private String displayName;
  private List<ServiceDefinitionType> serviceDefinitionTypes;
  private String category;
  private String yamlName;

  NGStepType(String displayName, List<ServiceDefinitionType> serviceDefinitionTypes, String category, String yamlName) {
    this.displayName = displayName;
    this.serviceDefinitionTypes = serviceDefinitionTypes;
    this.category = category;
    this.yamlName = yamlName;
  }

  @JsonCreator
  public static NGStepType getNGStepType(@JsonProperty("type") String yamlName) {
    for (NGStepType ngStepType : NGStepType.values()) {
      if (ngStepType.yamlName.equalsIgnoreCase(yamlName)) {
        return ngStepType;
      }
    }
    throw new IllegalArgumentException(
        String.format("Invalid value:%s, the expected values are: %s", yamlName, Arrays.toString(NGStepType.values())));
  }

  public static List<ServiceDefinitionType> getServiceDefinitionTypes(NGStepType ngStepType) {
    return ngStepType.serviceDefinitionTypes;
  }

  public static String getDisplayName(NGStepType ngStepType) {
    return ngStepType.displayName;
  }
  public static String getCategory(NGStepType ngStepType) {
    return ngStepType.category;
  }

  @JsonValue
  public String getYamlName(NGStepType ngStepType) {
    return ngStepType.yamlName;
  }
}
