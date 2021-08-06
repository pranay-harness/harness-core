package io.harness.accesscontrol.filter;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
class ScopeResourceTypes {
  public static final String ACCOUNT = "ACCOUNT";
  public static final String ORGANIZATION = "ORGANIZATION";
  public static final String PROJECT = "PROJECT";
}
