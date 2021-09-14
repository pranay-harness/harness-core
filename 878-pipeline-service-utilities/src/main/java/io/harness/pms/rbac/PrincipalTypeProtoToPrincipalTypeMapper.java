/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.rbac;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class PrincipalTypeProtoToPrincipalTypeMapper {
  public PrincipalType convertToAccessControlPrincipalType(io.harness.pms.contracts.plan.PrincipalType principalType) {
    switch (principalType) {
      case USER:
        return PrincipalType.USER;
      case USER_GROUP:
        return PrincipalType.USER_GROUP;
      case SERVICE:
        return PrincipalType.SERVICE;
      case API_KEY:
        return PrincipalType.API_KEY;
      default:
        throw new InvalidRequestException("Unknown principal type found");
    }
  }
}
