package io.harness;

import io.harness.beans.IdentifierRef;
import io.harness.beans.InputSetReference;
import io.harness.common.EntityReference;
import io.harness.common.EntityTypeConstants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// todo(abhinav): refactor/adapt this according to needs later depending on how service registration comes in
// one more enum might come in here for product types.
public enum EntityType {
  @JsonProperty(EntityTypeConstants.PROJECTS)
  PROJECTS(ModuleType.CORE, EntityTypeConstants.PROJECTS, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.PIPELINES)
  PIPELINES(ModuleType.CD, EntityTypeConstants.PIPELINES, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.CONNECTORS)
  CONNECTORS(ModuleType.CORE, EntityTypeConstants.CONNECTORS, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.SECRETS) SECRETS(ModuleType.CORE, EntityTypeConstants.SECRETS, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.SERVICE) SERVICE(ModuleType.CORE, EntityTypeConstants.SERVICE, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.ENVIRONMENT)
  ENVIRONMENT(ModuleType.CORE, EntityTypeConstants.ENVIRONMENT, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.INPUT_SETS)
  INPUT_SETS(ModuleType.CORE, EntityTypeConstants.INPUT_SETS, InputSetReference.class),
  @JsonProperty(EntityTypeConstants.CV_CONFIG)
  CV_CONFIG(ModuleType.CV, EntityTypeConstants.CV_CONFIG, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.DELEGATES)
  DELEGATES(ModuleType.CORE, EntityTypeConstants.DELEGATES, IdentifierRef.class),
  @JsonProperty(EntityTypeConstants.DELEGATE_CONFIGURATIONS)
  DELEGATE_CONFIGURATIONS(ModuleType.CORE, EntityTypeConstants.DELEGATE_CONFIGURATIONS, IdentifierRef.class);

  private final ModuleType moduleType;
  String yamlName;
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
    return Arrays.stream(EntityType.values())
        .filter(entityType -> entityType.moduleType.name().equalsIgnoreCase(moduleType.name()))
        .collect(Collectors.toList());
  }

  public static EntityType getEntityFromYamlType(String yamlType) {
    return Arrays.stream(EntityType.values())
        .filter(value -> value.yamlName.equalsIgnoreCase(yamlType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid value: " + yamlType));
  }

  public ModuleType getEntityProduct() {
    return this.moduleType;
  }

  EntityType(ModuleType moduleType, String yamlName, Class<? extends EntityReference> entityReferenceClass) {
    this.moduleType = moduleType;
    this.yamlName = yamlName;
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
