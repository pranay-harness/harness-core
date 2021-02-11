package io.harness.accesscontrol.scopes;

public interface Scope {
  String getKey();
  int getRank();
  String getIdentifierName();
}
