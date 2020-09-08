package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.harness.cdng.pipeline.stepinfo.StepSpecType;
import io.harness.cdng.service.beans.ServiceDefinitionType;

import java.util.Arrays;
import java.util.List;

/*
   Todo: Change StepSpecType.PLACEHOLDER to their respective type once the StepInfo for those is implemented.
 */
public enum NGStepType {
  // k8s steps
  @JsonProperty(StepSpecType.PLACEHOLDER)
  APPLY("Apply", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  SCALE("Scale", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  STAGE_DEPLOYMENT(
      "Stage Deployment", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.K8S_ROLLING_DEPLOY)
  K8S_ROLLING(
      "K8s Rolling", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecType.K8S_ROLLING_DEPLOY),
  @JsonProperty(StepSpecType.K8S_ROLLING_ROLLBACK)
  K8S_ROLLING_ROLLBACK("K8s Rolling Rollback", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes",
      StepSpecType.K8S_ROLLING_ROLLBACK),

  @JsonProperty(StepSpecType.PLACEHOLDER)
  SWAP_SELECTORS(
      "Swap Selectors", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  DELETE("Delete", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  CANARY_DEPLOYMENT(
      "Deployment", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes", StepSpecType.PLACEHOLDER),

  // Infrastructure Provisioners
  @JsonProperty(StepSpecType.PLACEHOLDER)
  TERRAFORM_APPLY("Terraform Apply", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Terraform", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  TERRAFORM_PROVISION("Terraform Provision", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Terraform", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  TERRAFORM_DELETE("Terraform Delete", Arrays.asList(ServiceDefinitionType.KUBERNETES),
      "Infrastructure Provisioners/Terraform", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  CREATE_STACK("Create Stack", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Cloudformation", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  DELETE_STACK("Delete Stack", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Cloudformation", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  SHELL_SCRIPT_PROVISIONER("Shell Script Provisioner", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Shell Script Provisioner", StepSpecType.PLACEHOLDER),

  // Issue Tracking
  @JsonProperty(StepSpecType.PLACEHOLDER)
  JIRA("Jira", Arrays.asList(ServiceDefinitionType.values()), "Issue Tracking", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  SERVICENOW("ServiceNow", Arrays.asList(ServiceDefinitionType.values()), "Issue Tracking", StepSpecType.PLACEHOLDER),
  // Notifications
  @JsonProperty(StepSpecType.PLACEHOLDER)
  EMAIL("Email", Arrays.asList(ServiceDefinitionType.values()), "Notification", StepSpecType.PLACEHOLDER),
  // Flow Control
  @JsonProperty(StepSpecType.PLACEHOLDER)
  BARRIERS("Barriers", Arrays.asList(ServiceDefinitionType.values()), "Flow Control", StepSpecType.PLACEHOLDER),
  // Utilities
  @JsonProperty(StepSpecType.SHELL_SCRIPT)
  SHELL_SCRIPT(
      "Shell Script", Arrays.asList(ServiceDefinitionType.values()), "Utilites/Scripted", StepSpecType.SHELL_SCRIPT),
  @JsonProperty(StepSpecType.HTTP)
  HTTP("Http", Arrays.asList(ServiceDefinitionType.values()), "Utilites/Scripted/", StepSpecType.HTTP),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  NEW_RELIC_DEPLOYMENT_MAKER("New Relic Deployment Maker", Arrays.asList(ServiceDefinitionType.values()),
      "Utilites/Non-Scripted/", StepSpecType.PLACEHOLDER),
  @JsonProperty(StepSpecType.PLACEHOLDER)
  TEMPLATIZED_SECRET_MANAGER("Templatized Secret Manager", Arrays.asList(ServiceDefinitionType.values()),
      "Utilites/Non-Scripted/", StepSpecType.PLACEHOLDER);

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
