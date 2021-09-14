/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import org.apache.commons.lang3.StringUtils;

/**
 * Account status.
 * @author rktummala on 08/30/18
 */
// Note: This is intentionally not made enum
@OwnedBy(DX)
@TargetModule(HarnessModule._955_ACCOUNT_MGMT)
public interface AccountStatus {
  String ACTIVE = "ACTIVE";
  String EXPIRED = "EXPIRED";
  String MARKED_FOR_DELETION = "MARKED-FOR-DELETION";
  String DELETED = "DELETED";
  String INACTIVE = "INACTIVE";

  static boolean isValid(String status) {
    if (isEmpty(status)) {
      return false;
    }

    switch (StringUtils.upperCase(status)) {
      case ACTIVE:
      case EXPIRED:
      case MARKED_FOR_DELETION:
      case DELETED:
      case INACTIVE:
        return true;
      default:
        return false;
    }
  }
}
