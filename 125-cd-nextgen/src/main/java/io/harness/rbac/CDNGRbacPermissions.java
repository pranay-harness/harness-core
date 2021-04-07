package io.harness.rbac;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

/**
 * A utility class to contain all the rbac permission for cdng
 */
@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class CDNGRbacPermissions {
  public String SERVICE_CREATE_PERMISSION = "core_service_edit";
  public String SERVICE_UPDATE_PERMISSION = "core_service_edit";
  public String SERVICE_RUNTIME_PERMISSION = "core_service_runtimeAccess";

  public String ENVIRONMENT_CREATE_PERMISSION = "core_environment_edit";
  public String ENVIRONMENT_RUNTIME_PERMISSION = "core_environment_runtimeAccess";
  public String ENVIRONMENT_UPDATE_PERMISSION = "core_environment_edit";
}
