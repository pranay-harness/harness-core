package io.harness.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AccessControlPermissions {
  public static final String VIEW_ROLE_PERMISSION = "core_role_view";
  public static final String EDIT_ROLE_PERMISSION = "core_role_edit";
  public static final String DELETE_ROLE_PERMISSION = "core_role_delete";
  public static final String VIEW_USER_PERMISSION = "core_user_view";
  public static final String VIEW_USERGROUP_PERMISSION = "core_usergroup_view";
  public static final String MANAGE_USERGROUP_PERMISSION = "core_usergroup_manage";
  public static final String MANAGE_USER_PERMISSION = "core_user_manage";
  public static final String VIEW_SERVICEACCOUNT_PERMISSION = "core_serviceaccount_view";
  public static final String EDIT_SERVICEACCOUNT_PERMISSION = "core_serviceaccount_edit";
}
