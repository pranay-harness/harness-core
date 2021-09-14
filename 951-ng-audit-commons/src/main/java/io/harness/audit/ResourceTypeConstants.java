/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  public static final String PIPELINE = "PIPELINE";
  public static final String TRIGGER = "TRIGGER";
  public static final String TEMPLATE = "TEMPLATE";
  public static final String INPUT_SET = "INPUT_SET";
  public static final String DELEGATE_CONFIGURATION = "DELEGATE_CONFIGURATION";
  public static final String SERVICE = "SERVICE";
  public static final String ENVIRONMENT = "ENVIRONMENT";
  public static final String DELEGATE = "DELEGATE";
  public static final String SERVICE_ACCOUNT = "SERVICE_ACCOUNT";
  public static final String CONNECTOR = "CONNECTOR";
  public static final String API_KEY = "API_KEY";
  public static final String TOKEN = "TOKEN";
}
