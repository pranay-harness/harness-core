package io.harness.gitsync.core;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// todo(abhinav): refactor/adapt this according to needs later depending on how service registration comes in
// one more enum might come in here for product types.
public enum EntityType {
  PROJECTS(Product.CORE),
  PIPELINES(Product.CD),
  CONNECTORS(Product.CORE);

  private final Product product;

  public static List<EntityType> getEntityTypes(Product product) {
    return Arrays.stream(EntityType.values())
        .filter(entityType -> entityType.product.name().equalsIgnoreCase(product.name()))
        .collect(Collectors.toList());
  }

  public static EntityType getEntityName(String entityType) {
    return EntityType.valueOf(entityType.toUpperCase());
  }

  public Product getEntityProduct() {
    return this.product;
  }

  EntityType(Product product) {
    this.product = product;
  }

  public String getEntityName() {
    return this.name().toLowerCase();
  }
}