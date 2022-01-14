package io.harness.resourcegroup.framework.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public final class ResourceGroupConstants {
  public static final String ACCOUNT = "ACCOUNT";
  public static final String ORGANIZATION = "ORGANIZATION";
  public static final String PROJECT = "PROJECT";
}
