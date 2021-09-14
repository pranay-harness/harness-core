/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.encryption;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Optional;

public enum Scope {
  @JsonProperty("account") ACCOUNT("account"),
  @JsonProperty("org") ORG("org"),
  @JsonProperty("project") PROJECT("project"),
  @JsonProperty("unknown") UNKNOWN("unknown");
  // TODO: mark UNKNOWN as JsonIgnore if Scope ever becomes visible in swagger
  private final String yamlRepresentation;

  public String getYamlRepresentation() {
    return yamlRepresentation;
  }

  Scope(String scopeStr) {
    this.yamlRepresentation = scopeStr;
  }

  public static Scope fromString(String scopeStr) {
    Optional<Scope> scopeOptional =
        Arrays.stream(Scope.values()).filter(scope -> scope.yamlRepresentation.equalsIgnoreCase(scopeStr)).findFirst();
    if (scopeOptional.isPresent()) {
      return scopeOptional.get();
    }
    throw new IllegalArgumentException("No scope found for string: " + scopeStr);
  }
}
