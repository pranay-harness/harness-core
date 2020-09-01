package io.harness.cdng.pipeline;

import io.harness.cdng.service.beans.ServiceDefinitionType;

import java.util.Arrays;
import java.util.List;

public enum NGStepType {
  // k8s steps
  APPLY("Apply", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes"),
  SCALE("Scale", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes"),
  STAGE_DEPLOYMENT("Stage Deployment", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes"),
  ROLLOUT_DEPLOYMENT("Rollout Deployment", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes"),
  K8S_ROLLING("K8s Rolling", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes"),
  SWAP_SELECTORS("Swap Selectors", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes"),
  DELETE("Delete", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes"),
  CANARY_DEPLOYMENT("Deployment", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Kubernetes"),

  // Infrastructure Provisioners
  TERRAFORM_APPLY(
      "Terraform Apply", Arrays.asList(ServiceDefinitionType.values()), "Infrastructure Provisioners/Terraform"),
  TERRAFORM_PROVISION(
      "Terraform Provision", Arrays.asList(ServiceDefinitionType.values()), "Infrastructure Provisioners/Terraform"),
  TERRAFORM_DELETE(
      "Terraform Delete", Arrays.asList(ServiceDefinitionType.KUBERNETES), "Infrastructure Provisioners/Terraform"),
  CREATE_STACK(
      "Create Stack", Arrays.asList(ServiceDefinitionType.values()), "Infrastructure Provisioners/Cloudformation"),
  DELETE_STACK(
      "Delete Stack", Arrays.asList(ServiceDefinitionType.values()), "Infrastructure Provisioners/Cloudformation"),
  SHELL_SCRIPT_PROVISIONER("Shell Script Provisioner", Arrays.asList(ServiceDefinitionType.values()),
      "Infrastructure Provisioners/Shell Script Provisioner"),

  // Issue Tracking
  JIRA("Jira", Arrays.asList(ServiceDefinitionType.values()), "Issue Tracking"),
  SERVICENOW("ServiceNow", Arrays.asList(ServiceDefinitionType.values()), "Issue Tracking"),

  // Notifications
  EMAIL("Email", Arrays.asList(ServiceDefinitionType.values()), "Notification"),

  // Flow Control
  BARRIERS("Barriers", Arrays.asList(ServiceDefinitionType.values()), "Flow Control"),

  // Utilities
  SHELL_SCRIPT("Shell Script", Arrays.asList(ServiceDefinitionType.values()), "Utilites/Scripted"),
  HTTP_CHECK("Http Check", Arrays.asList(ServiceDefinitionType.values()), "Utilites/Scripted/"),
  NEW_RELIC_DEPLOYMENT_MAKER(
      "New Relic Deployment Maker", Arrays.asList(ServiceDefinitionType.values()), "Utilites/Non-Scripted/"),
  TEMPLATIZED_SECRET_MANAGER(
      "Templatized Secret Manager", Arrays.asList(ServiceDefinitionType.values()), "Utilites/Non-Scripted/");

  private String displayName;
  private List<ServiceDefinitionType> serviceDefinitionTypes;
  private String category;

  NGStepType(String displayName, List<ServiceDefinitionType> serviceDefinitionTypes, String category) {
    this.displayName = displayName;
    this.serviceDefinitionTypes = serviceDefinitionTypes;
    this.category = category;
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
}
