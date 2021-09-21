package io.harness.accesscontrol.scopes;

import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(HarnessTeam.PL)
@Getter
public enum TestScopeLevels implements ScopeLevel {
  TEST_SCOPE("testScope", 0, "TEST_SCOPE");

  private final String name;
  private final int rank;
  private final String resourceType;

  TestScopeLevels(String name, int rank, String resourceType) {
    this.name = name;
    this.rank = rank;
    this.resourceType = resourceType;
  }

  @Override
  public String toString() {
    return name;
  }
}
