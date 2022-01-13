/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.scopes;

import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(HarnessTeam.PL)
@Getter
public enum TestScopeLevels implements ScopeLevel {
  TEST_SCOPE("testScope", 0, "TEST_SCOPE"),
  EXTRA_SCOPE("extraScope", 0, "EXTRA_SCOPE");

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
