/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.ownership;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface OwnedByAccount {
  /**
   * Delete objects if they belongs to account.
   *
   * @param accountId the app id
   *
   * NOTE: The account is not an object like the others. We need to delegate the objects deletion immediately.
   */
  void deleteByAccountId(String accountId);
}
