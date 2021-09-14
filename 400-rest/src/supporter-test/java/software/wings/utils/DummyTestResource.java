/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;

import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.ApiKeyAuthorized;
import software.wings.security.annotations.AuthRule;

@AuthRule(permissionType = LOGGED_IN)
@AuthRule(permissionType = ACCOUNT_MANAGEMENT)
@OwnedBy(PL)
public class DummyTestResource {
  @AuthRule(permissionType = MANAGE_DELEGATES)
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Void> testMultipleMethodAnnotations() {
    return new RestResponse<>();
  }

  // No annotations are added here, in order to test the class level ones
  public RestResponse<Void> testMultipleClassAnnotations() {
    return new RestResponse<>();
  }

  @ApiKeyAuthorized(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Void> testApiKeyAuthorizationAnnotation() {
    return new RestResponse<>();
  }

  @ApiKeyAuthorized(allowEmptyApiKey = true)
  public RestResponse<Void> testApiKeyAuthorizationAnnotationWithAllowEmptyApiKey() {
    return new RestResponse<>();
  }
}
