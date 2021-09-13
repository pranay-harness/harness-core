package io.harness.gitopsprovider.accesscontrol;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.GITOPS)
public class Permissions {
  public static final String VIEW_PROJECT_PERMISSION = "core_project_view";
  public static final String EDIT_PROJECT_PERMISSION = "core_project_edit";
  public static final String CREATE_PROJECT_PERMISSION = "core_project_create";
  public static final String DELETE_PROJECT_PERMISSION = "core_project_delete";
}
