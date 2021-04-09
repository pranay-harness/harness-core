package io.harness.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ResourceTypeConstants {
  public static final String ORGANIZATION = "organization";
  public static final String PROJECT = "project";
  public static final String USER_GROUP = "usergroup";
  public static final String SECRET = "secret";
  public static final String RESOURCE_GROUP = "resourcegroup";
}
