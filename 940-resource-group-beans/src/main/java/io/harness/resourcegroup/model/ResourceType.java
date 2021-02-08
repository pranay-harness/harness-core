package io.harness.resourcegroup.model;

import java.util.EnumSet;
import java.util.Set;

public enum ResourceType {
  ACCOUNT("ACCOUNT", EnumSet.of(Scope.ACCOUNT)),
  ORGANIZATION("ORGANIZATION", EnumSet.of(Scope.ACCOUNT)),
  PROJECT("PROJECT", EnumSet.of(Scope.ORGANIZATION)),
  PIPELINE("PIPELINE", EnumSet.of(Scope.PROJECT)),
  SERVICE("SERVICE", EnumSet.of(Scope.PROJECT)),
  CONNECTOR("CONNECTOR", EnumSet.of(Scope.ACCOUNT, Scope.ORGANIZATION, Scope.PROJECT)),
  SECRET_MANAGER("SECRET_MANAGER", EnumSet.of(Scope.ACCOUNT, Scope.ORGANIZATION, Scope.PROJECT)),
  SECRET("SECRET", EnumSet.of(Scope.ACCOUNT, Scope.ORGANIZATION, Scope.PROJECT)),
  ENVIRONMENT("ENVIRONMENT", EnumSet.of(Scope.PROJECT)),
  INPUT_SET("INPUT_SET", EnumSet.of(Scope.PROJECT));

  String name;
  Set<Scope> scopes;

  ResourceType(String name, Set<Scope> scopes) {
    this.name = name;
    this.scopes = scopes;
  }

  public Set<Scope> getScopes() {
    return this.scopes;
  }
}
