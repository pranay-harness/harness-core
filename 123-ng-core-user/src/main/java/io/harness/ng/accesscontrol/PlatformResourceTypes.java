/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class PlatformResourceTypes {
  public static final String ACCOUNT = "ACCOUNT";
  public static final String ORGANIZATION = "ORGANIZATION";
  public static final String PROJECT = "PROJECT";
  public static final String USERGROUP = "USERGROUP";
  public static final String USER = "USER";
  public static final String AUTHSETTING = "AUTHSETTING";
  public static final String SERVICEACCOUNT = "SERVICEACCOUNT";
}
