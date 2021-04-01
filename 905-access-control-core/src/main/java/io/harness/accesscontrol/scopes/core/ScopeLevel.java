package io.harness.accesscontrol.scopes.core;

public interface ScopeLevel {
  int getRank();
  String getParamName();
  String getResourceType();
}
