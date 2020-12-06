package software.wings.utils;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;

import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;

@AuthRule(permissionType = LOGGED_IN)
@AuthRule(permissionType = ACCOUNT_MANAGEMENT)
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
}
