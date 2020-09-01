package io.harness.gitsync.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// todo(abhinav): refactor/adapt this according to needs later depending on how service registration comes in
// one more enum might come in here for product types.
public enum EntityType {
  @JsonProperty("projects") PROJECTS(Product.CORE),
  @JsonProperty("pipelines") PIPELINES(Product.CD),
  @JsonProperty("connectors") CONNECTORS(Product.CORE);

  private final Product product;

  @JsonCreator
  public static EntityType fromString(@JsonProperty("entityType") String entityType) {
    for (EntityType entityTypeEnum : EntityType.values()) {
      if (entityTypeEnum.name().equalsIgnoreCase(entityType)) {
        return entityTypeEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + entityType);
  }

  public static List<EntityType> getEntityTypes(Product product) {
    return Arrays.stream(EntityType.values())
        .filter(entityType -> entityType.product.name().equalsIgnoreCase(product.name()))
        .collect(Collectors.toList());
  }

  public static EntityType getEntityDisplayName(String entityType) {
    return EntityType.valueOf(entityType.toUpperCase());
  }

  public Product getEntityProduct() {
    return this.product;
  }

  EntityType(Product product) {
    this.product = product;
  }

  public String getEntityDisplayName() {
    return this.name().toLowerCase();
  }
}