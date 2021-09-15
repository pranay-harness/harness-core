/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.acl.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.PermissionCheck;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(PL)
public interface ACLDAO {
  List<Boolean> checkForAccess(Principal principal, List<PermissionCheck> permissionChecks);
}
