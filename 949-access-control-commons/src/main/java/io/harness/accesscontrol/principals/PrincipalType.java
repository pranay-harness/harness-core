/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.principals;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum PrincipalType {
  USER,
  USER_GROUP,
  SERVICE,
  API_KEY,
  SERVICE_ACCOUNT;

  public static PrincipalType fromSecurityPrincipalType(io.harness.security.dto.PrincipalType principalType) {
    switch (principalType) {
      case SERVICE:
        return SERVICE;
      case USER:
        return USER;
      case API_KEY:
        return API_KEY;
      case SERVICE_ACCOUNT:
        return SERVICE_ACCOUNT;
      default:
        return null;
    }
  }
}
