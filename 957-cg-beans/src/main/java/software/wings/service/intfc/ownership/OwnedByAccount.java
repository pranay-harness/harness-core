/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
