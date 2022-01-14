package io.harness.resourcegroup;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ResourceGroupPermissions {
  public static final String VIEW_RESOURCEGROUP_PERMISSION = "core_resourcegroup_view";
  public static final String EDIT_RESOURCEGROUP_PERMISSION = "core_resourcegroup_edit";
  public static final String DELETE_RESOURCEGROUP_PERMISSION = "core_resourcegroup_delete";
}
