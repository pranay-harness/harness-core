package io.harness.accesscontrol.scopes;

import java.util.Map;
import javax.validation.constraints.NotNull;

public interface ScopeService {
  String SCOPES_BY_IDENTIFIER_NAME = "scopesByIdentifierName";
  String SCOPES_BY_KEY = "scopesByKey";

  Map<String, Scope> getAllScopesByKey();

  Scope getLowestScope(@NotNull Map<String, String> scopeIdentifiers);

  String getFullyQualifiedPath(@NotNull Map<String, String> scopeIdentifiers);

  Map<String, String> getIdentifiers(String scopeIdentifier);
}
