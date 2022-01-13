/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.instance.licensing;

public interface InstanceUsageLimitChecker {
  /**
   * Tells whether the instance usage of a given account is within the limits of license or not.
   * If
   *
   *   maxAllowedUsage = L (for limit), usage = U
   *
   * then this method will return true if
   *
   *    U < (percentLimit/100.0) * L
   *
   * @param accountId account Id for which to check limits
   * @return whether usage is within limits or not
   */
  boolean isWithinLimit(String accountId, long percentLimit, double actualUsage);
}
