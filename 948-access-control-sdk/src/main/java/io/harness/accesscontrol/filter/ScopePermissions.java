/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.filter;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
class ScopePermissions {
  public static final String VIEW_ORGANIZATION_PERMISSION = "core_organization_view";
  public static final String VIEW_PROJECT_PERMISSION = "core_project_view";
  public static final String VIEW_ACCOUNT_PERMISSION = "core_account_view";
}
