package io.harness.accesscontrol.scopes.harness;

import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ACCOUNT_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ORG_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.PROJECT_LEVEL_PARAM_NAME;

import io.harness.accesscontrol.scopes.core.ScopeLevel;

import lombok.Getter;

@Getter
public enum HarnessScopeLevel implements ScopeLevel {
  ACCOUNT("account", 0, ACCOUNT_LEVEL_PARAM_NAME, "ACCOUNT"),
  ORGANIZATION("organization", 1, ORG_LEVEL_PARAM_NAME, "ORGANIZATION"),
  PROJECT("project", 2, PROJECT_LEVEL_PARAM_NAME, "PROJECT");

  private final String name;
  private final int rank;
  private final String paramName;
  private final String resourceType;

  HarnessScopeLevel(String name, int rank, String paramName, String resourceType) {
    this.name = name;
    this.rank = rank;
    this.paramName = paramName;
    this.resourceType = resourceType;
  }

  @Override
  public String toString() {
    return name;
  }
}
