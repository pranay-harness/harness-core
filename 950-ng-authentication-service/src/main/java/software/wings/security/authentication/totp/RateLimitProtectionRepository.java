/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.totp;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface RateLimitProtectionRepository {
  /**
   * Creates {@link RateLimitProtection} record if it does not exist already
   */
  void createRateLimitProtectionDataIfNotExists(String userUuid);

  /**
   * Removes all access times that are smaller than the given least allowed tim
   */
  RateLimitProtection pruneIncorrectAttemptTimes(String userUuid, Long leastAllowedTime);

  /**
   * Adds the incorrect timestamp.
   */
  RateLimitProtection addIncorrectAttempt(String userUuid, Long time);

  /**
   * Updates the time last email was sent to the user and returns the previous version.
   *
   * Needs to be atomic
   *
   * @param userUuid the UUID of the user whose email timestamp we are updating
   * @return previous timestamp when the last email was sent
   */
  long getAndUpdateLastEmailSentToUserAt(String userUuid, Long newTimestamp);

  /**
   * Updates the time last email was sent to the SecOps and returns the previous version.
   *
   * Needs to be atomic
   *
   * @param userUuid the UUID of the user we need to alert SecOps about
   * @return previous timestamp when the last email was sent
   */
  long getAndUpdateLastEmailSentToSecOpsAt(String userUuid, Long newTimestamp);
}
