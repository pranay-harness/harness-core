package io.harness.encryption;

import java.util.Arrays;
import java.util.Optional;

public enum Scope {
  ACCOUNT("acc"),
  ORG("org"),
  PROJECT("proj");
  private final String representation;

  Scope(String scopeStr) {
    this.representation = scopeStr;
  }

  public static Scope fromString(String scopeStr) {
    Optional<Scope> scopeOptional =
        Arrays.stream(Scope.values()).filter(scope -> scope.representation.equalsIgnoreCase(scopeStr)).findFirst();
    if (scopeOptional.isPresent()) {
      return scopeOptional.get();
    }
    throw new IllegalArgumentException("No scope found for string: " + scopeStr);
  }
}
