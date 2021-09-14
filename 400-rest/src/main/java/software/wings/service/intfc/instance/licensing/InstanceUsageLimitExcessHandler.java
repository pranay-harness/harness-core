/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.instance.licensing;

public interface InstanceUsageLimitExcessHandler {
  /**
   * Checks whether given accounts instance usage is within limit.
   * And takes appropriate action otherwise.
   *
   * @param accountId
   */
  void handle(String accountId, double actualUsage);

  /**
   * Violation: If am account's deployed SI usage is more than allowed usage, it is considered a violation.
   * This method updates the count of that violation in store (Mongo at present, but can be anything else depending upon
   * implementation)
   */
  void updateViolationCount(String accountId, double actualUsage);
}
