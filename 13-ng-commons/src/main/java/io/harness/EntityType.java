package io.harness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// todo(abhinav): refactor/adapt this according to needs later depending on how service registration comes in
// one more enum might come in here for product types.
public enum EntityType {
  @JsonProperty("projects") PROJECTS(ModuleType.CORE),
  @JsonProperty("pipelines") PIPELINES(ModuleType.CD),
  @JsonProperty("connectors") CONNECTORS(ModuleType.CORE),
  @JsonProperty("secrets") SECRETS(ModuleType.CORE);

  private final ModuleType moduleType;

  @JsonCreator
  public static EntityType fromString(@JsonProperty("entityType") String entityType) {
    for (EntityType entityTypeEnum : EntityType.values()) {
      if (entityTypeEnum.name().equalsIgnoreCase(entityType)) {
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

  public static EntityType getEntityDisplayName(String entityType) {
    return EntityType.valueOf(entityType.toUpperCase());
  }

  public ModuleType getEntityProduct() {
    return this.moduleType;
  }

  EntityType(ModuleType moduleType) {
    this.moduleType = moduleType;
  }

  public String getEntityDisplayName() {
    return this.name().toLowerCase();
  }
}