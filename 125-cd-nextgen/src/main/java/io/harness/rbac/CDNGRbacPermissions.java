/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  public String SERVICE_RUNTIME_PERMISSION = "core_service_access";
  public String SERVICE_VIEW_PERMISSION = "core_service_view";

  public String ENVIRONMENT_CREATE_PERMISSION = "core_environment_edit";
  public String ENVIRONMENT_RUNTIME_PERMISSION = "core_environment_access";
  public String ENVIRONMENT_UPDATE_PERMISSION = "core_environment_edit";
  public String ENVIRONMENT_VIEW_PERMISSION = "core_environment_view";
}
