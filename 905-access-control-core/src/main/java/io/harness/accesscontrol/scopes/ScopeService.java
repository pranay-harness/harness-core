package io.harness.accesscontrol.scopes;

import java.util.Map;
import javax.validation.constraints.NotNull;

public interface ScopeService {
  String SCOPES_BY_IDENTIFIER_KEY = "scopesByIdentifierKey";
  String SCOPES_BY_PATH_KEY = "scopesByPathKey";

  Scope getScope(@NotNull Map<String, String> scopeIdentifiers);

  String getScopeIdentifier(@NotNull Map<String, String> scopeIdentifiers);

  Map<String, String> getIdentifiers(String scopeIdentifier);
}
