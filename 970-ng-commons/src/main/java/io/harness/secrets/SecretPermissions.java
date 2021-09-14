/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secrets;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class SecretPermissions {
  public static final String SECRET_VIEW_PERMISSION = "core_secret_view";
  public static final String SECRET_RESOURCE_TYPE = "SECRET";
  public static final String SECRET_DELETE_PERMISSION = "core_secret_delete";
  public static final String SECRET_ACCESS_PERMISSION = "core_secret_access";
  public static final String SECRET_EDIT_PERMISSION = "core_secret_edit";
}
