package io.harness.accesscontrol.scopes;

public interface Scope {
  String getPathKey();
  String getDBKey();
  int getRank();
  String getIdentifierKey();
}
