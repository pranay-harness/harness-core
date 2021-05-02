package io.harness.ng.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class PlatformPermissions {
  public static final String DELETE_ORGANIZATION_PERMISSION = "core_organization_delete";
  public static final String VIEW_ORGANIZATION_PERMISSION = "core_organization_view";
  public static final String EDIT_ORGANIZATION_PERMISSION = "core_organization_edit";
  public static final String CREATE_ORGANIZATION_PERMISSION = "core_organization_create";
  public static final String DELETE_PROJECT_PERMISSION = "core_project_delete";
  public static final String VIEW_PROJECT_PERMISSION = "core_project_view";
  public static final String EDIT_PROJECT_PERMISSION = "core_project_edit";
  public static final String CREATE_PROJECT_PERMISSION = "core_project_create";
  public static final String VIEW_USERGROUP_PERMISSION = "core_usergroup_view";
  public static final String MANAGE_USERGROUP_PERMISSION = "core_usergroup_manage";
  public static final String VIEW_USER_PERMISSION = "core_user_view";
  public static final String MANAGE_USER_PERMISSION = "core_user_manage";
}