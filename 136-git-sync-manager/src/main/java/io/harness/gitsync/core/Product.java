package io.harness.gitsync.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum Product {
  @JsonProperty("cd") CD,
  @JsonProperty("ci") CI,
  @JsonProperty("core") CORE,
  @JsonProperty("cv") CV;

  @JsonCreator
  public static Product fromString(@JsonProperty("product") String product) {
    for (Product productEnum : Product.values()) {
      if (productEnum.name().equalsIgnoreCase(product)) {
        return productEnum;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + product);
  }
}
