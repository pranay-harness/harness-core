package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.InputSetReference;
import io.harness.beans.TriggerReference;
import io.harness.common.EntityReference;
import io.harness.common.EntityTypeConstants;
import io.harness.common.EntityYamlRootNames;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.DX)
// todo(abhinav): refactor/adapt this according to needs later depending on how service registration comes in
// one more enum might come in here for product types.
public enum EntityType {
  @JsonProperty(EntityTypeConstants.PROJECTS)
  PROJECTS(ModuleType.CORE, EntityTypeConstants.PROJECTS, EntityYamlRootNames.PROJECT, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.PIPELINES)
  PIPELINES(ModuleType.CD, EntityTypeConstants.PIPELINES, EntityYamlRootNames.PIPELINE, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.PIPELINE_STEPS)
  PIPELINE_STEPS(
      ModuleType.CD, EntityTypeConstants.PIPELINE_STEPS, EntityYamlRootNames.PIPELINE_STEP, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.CONNECTORS)
  CONNECTORS(ModuleType.CORE, EntityTypeConstants.CONNECTORS, EntityYamlRootNames.CONNECTOR, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.SECRETS)
  SECRETS(ModuleType.CORE, EntityTypeConstants.SECRETS, EntityYamlRootNames.SECRET, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.SERVICE)
  SERVICE(ModuleType.CORE, EntityTypeConstants.SERVICE, EntityYamlRootNames.SERVICE, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.ENVIRONMENT)
  ENVIRONMENT(ModuleType.CORE, EntityTypeConstants.ENVIRONMENT, EntityYamlRootNames.ENVIRONMENT, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.INPUT_SETS)
  INPUT_SETS(ModuleType.CORE, EntityTypeConstants.INPUT_SETS, EntityYamlRootNames.INPUT_SET, InputSetReference.class),
  @JsonProperty(EntityTypeConstants.CV_CONFIG)
  CV_CONFIG(ModuleType.CV, EntityTypeConstants.CV_CONFIG, EntityYamlRootNames.CV_CONFIG, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.DELEGATES)
  DELEGATES(ModuleType.CORE, EntityTypeConstants.DELEGATES, EntityYamlRootNames.DELEGATE, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.DELEGATE_CONFIGURATIONS)
  DELEGATE_CONFIGURATIONS(ModuleType.CORE, EntityTypeConstants.DELEGATE_CONFIGURATIONS,
      EntityYamlRootNames.DELEGATE_CONFIGURATION, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.CV_VERIFICATION_JOB)
  CV_VERIFICATION_JOB(ModuleType.CV, EntityTypeConstants.CV_VERIFICATION_JOB, EntityYamlRootNames.CV_VERIFICATION_JOB,
      IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.INTEGRATION_STAGE)
  INTEGRATION_STAGE(
      ModuleType.CI, EntityTypeConstants.INTEGRATION_STAGE, EntityYamlRootNames.INTEGRATION_STAGE, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.INTEGRATION_STEPS)
  INTEGRATION_STEPS(
      ModuleType.CI, EntityTypeConstants.INTEGRATION_STEPS, EntityYamlRootNames.INTEGRATION_STEP, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.CV_KUBERNETES_ACTIVITY_SOURCE)
  CV_KUBERNETES_ACTIVITY_SOURCE(ModuleType.CV, EntityTypeConstants.CV_KUBERNETES_ACTIVITY_SOURCE,
      EntityYamlRootNames.CV_KUBERNETES_ACTIVITY_SOURCE, IdentifierRef.class),

  @JsonProperty(EntityTypeConstants.DEPLOYMENT_STEPS)
  DEPLOYMENT_STEPS(
      ModuleType.CD, EntityTypeConstants.DEPLOYMENT_STEPS, EntityYamlRootNames.DEPLOYMENT_STEP, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.DEPLOYMENT_STAGE)
  DEPLOYMENT_STAGE(
      ModuleType.CD, EntityTypeConstants.DEPLOYMENT_STAGE, EntityYamlRootNames.DEPLOYMENT_STAGE, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.APPROVAL_STAGE)
  APPROVAL_STAGE(
      ModuleType.CD, EntityTypeConstants.APPROVAL_STAGE, EntityYamlRootNames.APPROVAL_STAGE, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.FEATURE_FLAG_STAGE)
  FEATURE_FLAG_STAGE(ModuleType.CF, EntityTypeConstants.FEATURE_FLAG_STAGE, EntityYamlRootNames.FEATURE_FLAG_STAGE,
      IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.TRIGGERS)
  TRIGGERS(ModuleType.CD, EntityTypeConstants.TRIGGERS, EntityYamlRootNames.TRIGGERS, TriggerReference.class);

  private final ModuleType moduleType;
  String yamlName;
  String yamlRootElementString;
  Class<? extends EntityReference> entityReferenceClass;

  @JsonCreator
  public static EntityType fromString(@JsonProperty("type") String entityType) {
    for (EntityType entityTypeEnum : EntityType.values()) {
      if (entityTypeEnum.getYamlName().equalsIgnoreCase(entityType)) {
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
        .filter(value -> value.yamlRootElementString.equalsIgnoreCase(yamlRootString))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid value: " + yamlRootString));
  }

  public ModuleType getEntityProduct() {
    return this.moduleType;
  }

  EntityType(ModuleType moduleType, String yamlName, String yamlRootElementString,
      Class<? extends EntityReference> entityReferenceClass) {
    this.moduleType = moduleType;
    this.yamlName = yamlName;
    this.yamlRootElementString = yamlRootElementString;
    this.entityReferenceClass = entityReferenceClass;
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
