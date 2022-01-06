/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.InputSetReference;
import io.harness.beans.NGTemplateReference;
import io.harness.beans.TriggerReference;
import io.harness.common.EntityReference;
import io.harness.common.EntityTypeConstants;
import io.harness.common.EntityYamlRootNames;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.DX)
// todo(abhinav): refactor/adapt this according to needs later depending on how service registration comes in
// one more enum might come in here for product types.
public enum EntityType {
  @JsonProperty(EntityTypeConstants.PROJECTS)
  PROJECTS(ModuleType.CORE, EntityTypeConstants.PROJECTS, IdentifierRef.class, EntityYamlRootNames.PROJECT),
  @JsonProperty(EntityTypeConstants.PIPELINES)
  PIPELINES(ModuleType.CD, EntityTypeConstants.PIPELINES, IdentifierRef.class, EntityYamlRootNames.PIPELINE),
  @JsonProperty(EntityTypeConstants.PIPELINE_STEPS)
  PIPELINE_STEPS(
      ModuleType.CD, EntityTypeConstants.PIPELINE_STEPS, IdentifierRef.class, EntityYamlRootNames.PIPELINE_STEP),
  @JsonProperty(EntityTypeConstants.HTTP)
  HTTP_STEP(ModuleType.PMS, EntityTypeConstants.HTTP, IdentifierRef.class, EntityYamlRootNames.HTTP),
  @JsonProperty(EntityTypeConstants.JIRA_CREATE)
  JIRA_CREATE_STEP(
      ModuleType.PMS, EntityTypeConstants.JIRA_CREATE, IdentifierRef.class, EntityYamlRootNames.JIRA_CREATE),
  @JsonProperty(EntityTypeConstants.JIRA_UPDATE)
  JIRA_UPDATE_STEP(
      ModuleType.PMS, EntityTypeConstants.JIRA_UPDATE, IdentifierRef.class, EntityYamlRootNames.JIRA_UPDATE),
  @JsonProperty(EntityTypeConstants.JIRA_APPROVAL)
  JIRA_APPROVAL_STEP(
      ModuleType.PMS, EntityTypeConstants.JIRA_APPROVAL, IdentifierRef.class, EntityYamlRootNames.JIRA_APPROVAL),
  @JsonProperty(EntityTypeConstants.HARNESS_APPROVAL)
  HARNESS_APPROVAL_STEP(
      ModuleType.PMS, EntityTypeConstants.HARNESS_APPROVAL, IdentifierRef.class, EntityYamlRootNames.HARNESS_APPROVAL),
  @JsonProperty(EntityTypeConstants.BARRIER)
  BARRIER_STEP(ModuleType.PMS, EntityTypeConstants.BARRIER, IdentifierRef.class, EntityYamlRootNames.BARRIER),
  @JsonProperty(EntityTypeConstants.SHELL_SCRIPT)
  SHELL_SCRIPT_STEP(
      ModuleType.PMS, EntityTypeConstants.SHELL_SCRIPT, IdentifierRef.class, EntityYamlRootNames.SHELL_SCRIPT),
  @JsonProperty(EntityTypeConstants.K8S_CANARY_DEPLOY)
  K8S_CANARY_DEPLOY_STEP(
      ModuleType.CD, EntityTypeConstants.K8S_CANARY_DEPLOY, IdentifierRef.class, EntityYamlRootNames.K8S_CANARY_DEPLOY),
  @JsonProperty(EntityTypeConstants.K8S_APPLY)
  K8S_APPLY_STEP(ModuleType.CD, EntityTypeConstants.K8S_APPLY, IdentifierRef.class, EntityYamlRootNames.K8S_APPLY),
  @JsonProperty(EntityTypeConstants.K8S_BLUE_GREEN_DEPLOY)
  K8S_BLUE_GREEN_DEPLOY_STEP(ModuleType.CD, EntityTypeConstants.K8S_BLUE_GREEN_DEPLOY, IdentifierRef.class,
      EntityYamlRootNames.K8S_BLUE_GREEN_DEPLOY),
  @JsonProperty(EntityTypeConstants.K8S_ROLLING_DEPLOY)
  K8S_ROLLING_DEPLOY_STEP(ModuleType.CD, EntityTypeConstants.K8S_ROLLING_DEPLOY, IdentifierRef.class,
      EntityYamlRootNames.K8S_ROLLING_DEPLOY),
  @JsonProperty(EntityTypeConstants.K8S_ROLLING_ROLLBACK)
  K8S_ROLLING_ROLLBACK_STEP(ModuleType.CD, EntityTypeConstants.K8S_ROLLING_ROLLBACK, IdentifierRef.class,
      EntityYamlRootNames.K8S_ROLLING_ROLLBACK),
  @JsonProperty(EntityTypeConstants.K8S_SCALE)
  K8S_SCALE_STEP(ModuleType.CD, EntityTypeConstants.K8S_SCALE, IdentifierRef.class, EntityYamlRootNames.K8S_SCALE),
  @JsonProperty(EntityTypeConstants.K8S_DELETE)
  K8S_DELETE_STEP(ModuleType.CD, EntityTypeConstants.K8S_DELETE, IdentifierRef.class, EntityYamlRootNames.K8S_DELETE),
  @JsonProperty(EntityTypeConstants.K8S_BG_SWAP_SERVICES)
  K8S_BG_SWAP_SERVICES_STEP(ModuleType.CD, EntityTypeConstants.K8S_BG_SWAP_SERVICES, IdentifierRef.class,
      EntityYamlRootNames.K8S_SWAP_SERVICES),
  @JsonProperty(EntityTypeConstants.K8S_CANARY_DELETE)
  K8S_CANARY_DELETE_STEP(
      ModuleType.CD, EntityTypeConstants.K8S_CANARY_DELETE, IdentifierRef.class, EntityYamlRootNames.K8S_CANARY_DELETE),
  @JsonProperty(EntityTypeConstants.TERRAFORM_APPLY)
  TERRAFORM_APPLY_STEP(
      ModuleType.CD, EntityTypeConstants.TERRAFORM_APPLY, IdentifierRef.class, EntityYamlRootNames.TERRAFORM_APPLY),
  @JsonProperty(EntityTypeConstants.TERRAFORM_PLAN)
  TERRAFORM_PLAN_STEP(
      ModuleType.CD, EntityTypeConstants.TERRAFORM_PLAN, IdentifierRef.class, EntityYamlRootNames.TERRAFORM_PLAN),
  @JsonProperty(EntityTypeConstants.TERRAFORM_DESTROY)
  TERRAFORM_DESTROY_STEP(
      ModuleType.CD, EntityTypeConstants.TERRAFORM_DESTROY, IdentifierRef.class, EntityYamlRootNames.TERRAFORM_DESTROY),
  @JsonProperty(EntityTypeConstants.TERRAFORM_ROLLBACK)
  TERRAFORM_ROLLBACK_STEP(ModuleType.CD, EntityTypeConstants.TERRAFORM_ROLLBACK, IdentifierRef.class,
      EntityYamlRootNames.TERRAFORM_ROLLBACK),
  @JsonProperty(EntityTypeConstants.HELM_DEPLOY)
  HELM_DEPLOY_STEP(
      ModuleType.CD, EntityTypeConstants.HELM_DEPLOY, IdentifierRef.class, EntityYamlRootNames.HELM_DEPLOY),
  @JsonProperty(EntityTypeConstants.HELM_ROLLBACK)
  HELM_ROLLBACK_STEP(
      ModuleType.CD, EntityTypeConstants.HELM_ROLLBACK, IdentifierRef.class, EntityYamlRootNames.HELM_ROLLBACK),
  @JsonProperty(EntityTypeConstants.CONNECTORS)
  CONNECTORS(ModuleType.CORE, EntityTypeConstants.CONNECTORS, IdentifierRef.class, EntityYamlRootNames.CONNECTOR),
  @JsonProperty(EntityTypeConstants.SECRETS)
  SECRETS(ModuleType.CORE, EntityTypeConstants.SECRETS, IdentifierRef.class, EntityYamlRootNames.SECRET),
  @JsonProperty(EntityTypeConstants.SERVICE)
  SERVICE(ModuleType.CORE, EntityTypeConstants.SERVICE, IdentifierRef.class, EntityYamlRootNames.SERVICE),
  @JsonProperty(EntityTypeConstants.ENVIRONMENT)
  ENVIRONMENT(ModuleType.CORE, EntityTypeConstants.ENVIRONMENT, IdentifierRef.class, EntityYamlRootNames.ENVIRONMENT),
  @JsonProperty(EntityTypeConstants.INPUT_SETS)
  INPUT_SETS(ModuleType.CORE, EntityTypeConstants.INPUT_SETS, InputSetReference.class, EntityYamlRootNames.INPUT_SET,
      EntityYamlRootNames.OVERLAY_INPUT_SET),
  @JsonProperty(EntityTypeConstants.CV_CONFIG)
  CV_CONFIG(ModuleType.CV, EntityTypeConstants.CV_CONFIG, IdentifierRef.class, EntityYamlRootNames.CV_CONFIG),
  @JsonProperty(EntityTypeConstants.DELEGATES)
  DELEGATES(ModuleType.CORE, EntityTypeConstants.DELEGATES, IdentifierRef.class, EntityYamlRootNames.DELEGATE),
  @JsonProperty(EntityTypeConstants.DELEGATE_CONFIGURATIONS)
  DELEGATE_CONFIGURATIONS(ModuleType.CORE, EntityTypeConstants.DELEGATE_CONFIGURATIONS, IdentifierRef.class,
      EntityYamlRootNames.DELEGATE_CONFIGURATION),
  @JsonProperty(EntityTypeConstants.CV_VERIFICATION_JOB)
  CV_VERIFICATION_JOB(ModuleType.CV, EntityTypeConstants.CV_VERIFICATION_JOB, IdentifierRef.class,
      EntityYamlRootNames.CV_VERIFICATION_JOB),
  @JsonProperty(EntityTypeConstants.INTEGRATION_STAGE)
  INTEGRATION_STAGE(
      ModuleType.CI, EntityTypeConstants.INTEGRATION_STAGE, IdentifierRef.class, EntityYamlRootNames.INTEGRATION_STAGE),
  @JsonProperty(EntityTypeConstants.INTEGRATION_STEPS)
  INTEGRATION_STEPS(
      ModuleType.CI, EntityTypeConstants.INTEGRATION_STEPS, IdentifierRef.class, EntityYamlRootNames.INTEGRATION_STEP),
  @JsonProperty(EntityTypeConstants.CV_KUBERNETES_ACTIVITY_SOURCE)
  CV_KUBERNETES_ACTIVITY_SOURCE(ModuleType.CV, EntityTypeConstants.CV_KUBERNETES_ACTIVITY_SOURCE, IdentifierRef.class,
      EntityYamlRootNames.CV_KUBERNETES_ACTIVITY_SOURCE),

  @JsonProperty(EntityTypeConstants.DEPLOYMENT_STEPS)
  DEPLOYMENT_STEPS(
      ModuleType.CD, EntityTypeConstants.DEPLOYMENT_STEPS, IdentifierRef.class, EntityYamlRootNames.DEPLOYMENT_STEP),
  @JsonProperty(EntityTypeConstants.DEPLOYMENT_STAGE)
  DEPLOYMENT_STAGE(
      ModuleType.CD, EntityTypeConstants.DEPLOYMENT_STAGE, IdentifierRef.class, EntityYamlRootNames.DEPLOYMENT_STAGE),
  @JsonProperty(EntityTypeConstants.APPROVAL_STAGE)
  APPROVAL_STAGE(
      ModuleType.PMS, EntityTypeConstants.APPROVAL_STAGE, IdentifierRef.class, EntityYamlRootNames.APPROVAL_STAGE),
  @JsonProperty(EntityTypeConstants.FEATURE_FLAG_STAGE)
  FEATURE_FLAG_STAGE(ModuleType.CF, EntityTypeConstants.FEATURE_FLAG_STAGE, IdentifierRef.class,
      EntityYamlRootNames.FEATURE_FLAG_STAGE),
  @JsonProperty(EntityTypeConstants.TEMPLATE)
  TEMPLATE(ModuleType.TEMPLATESERVICE, EntityTypeConstants.TEMPLATE, NGTemplateReference.class,
      EntityYamlRootNames.TEMPLATE),
  @JsonProperty(EntityTypeConstants.TRIGGERS)
  TRIGGERS(ModuleType.CD, EntityTypeConstants.TRIGGERS, TriggerReference.class, EntityYamlRootNames.TRIGGERS),
  @JsonProperty(EntityTypeConstants.MONITORED_SERVICE)
  MONITORED_SERVICE(
      ModuleType.CV, EntityTypeConstants.MONITORED_SERVICE, IdentifierRef.class, EntityYamlRootNames.MONITORED_SERVICE),
  @JsonProperty(EntityTypeConstants.GIT_REPOSITORIES)
  GIT_REPOSITORIES(
      ModuleType.CORE, EntityTypeConstants.GIT_REPOSITORIES, IdentifierRef.class, EntityYamlRootNames.GIT_REPOSITORY),
  @JsonProperty(EntityTypeConstants.FEATURE_FLAGS)
  FEATURE_FLAGS(
      ModuleType.CF, EntityTypeConstants.FEATURE_FLAGS, IdentifierRef.class, EntityYamlRootNames.FEATURE_FLAGS),
  @JsonProperty(EntityTypeConstants.SERVICENOW_APPROVAL)
  SERVICENOW_APPROVAL_STEP(ModuleType.CD, EntityTypeConstants.SERVICENOW_APPROVAL, IdentifierRef.class,
      EntityYamlRootNames.SERVICENOW_APPROVAL);

  private final ModuleType moduleType;
  String yamlName;
  List<String> yamlRootElementString;
  Class<? extends EntityReference> entityReferenceClass;

  @JsonCreator
  public static EntityType fromString(@JsonProperty("type") String entityType) {
    for (EntityType entityTypeEnum : EntityType.values()) {
      if (entityTypeEnum.getYamlName().equalsIgnoreCase(entityType)) {
        return entityTypeEnum;
      }
      if (entityTypeEnum.name().equalsIgnoreCase(entityType)) {
        return entityTypeEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + entityType);
  }

  public static List<EntityType> getEntityTypes(ModuleType moduleType) {
    return (moduleType == null)
        ? Collections.emptyList()
        : Arrays.stream(EntityType.values())
              .filter(entityType -> entityType.moduleType.name().equalsIgnoreCase(moduleType.name()))
              .collect(Collectors.toList());
  }

  public static EntityType getEntityFromYamlType(String yamlType) {
    return Arrays.stream(EntityType.values())
        .filter(value -> value.yamlName.equalsIgnoreCase(yamlType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid value: " + yamlType));
  }

  public static EntityType getEntityTypeFromYamlRootName(String yamlRootString) {
    return Arrays.stream(EntityType.values())
        .filter(entityType -> entityType.yamlRootElementString.stream().anyMatch(yamlRootString::equalsIgnoreCase))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid value: " + yamlRootString));
  }

  public ModuleType getEntityProduct() {
    return this.moduleType;
  }

  EntityType(ModuleType moduleType, String yamlName, Class<? extends EntityReference> entityReferenceClass,
      String... yamlRootStrings) {
    this.moduleType = moduleType;
    this.yamlName = yamlName;
    this.entityReferenceClass = entityReferenceClass;
    this.yamlRootElementString = Lists.newArrayList(yamlRootStrings);
  }

  public String getYamlName() {
    return yamlName;
  }

  @Override
  @JsonValue
  public String toString() {
    return this.yamlName;
  }
}
