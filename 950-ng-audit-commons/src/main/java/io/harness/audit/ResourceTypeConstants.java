package io.harness.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ResourceTypeConstants {
  public static final String ORGANIZATION = "ORGANIZATION";
  public static final String PROJECT = "PROJECT";
  public static final String USER_GROUP = "USER_GROUP";
  public static final String SECRET = "SECRET";
  public static final String RESOURCE_GROUP = "RESOURCE_GROUP";
  public static final String USER = "USER";
  public static final String ROLE = "ROLE";
  public static final String ROLE_ASSIGNMENT = "ROLE_ASSIGNMENT";
}
