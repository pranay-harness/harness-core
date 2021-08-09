package io.harness.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AccessControlResourceTypes {
  public static final String ROLE = "ROLE";
  public static final String USER = "USER";
  public static final String USER_GROUP = "USERGROUP";
  public static final String SERVICEACCOUNT = "SERVICEACCOUNT";
}
