/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.limits;

public interface LimitVicinityHandler {
  /**
   * Checks if a particular account is approaching limits consumption and takes appropriate actions (like raising
   * alerts) in that case
   */
  void checkAndAct(String accountId);
}
