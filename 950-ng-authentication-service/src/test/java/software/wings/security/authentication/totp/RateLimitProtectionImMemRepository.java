/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.totp;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

@OwnedBy(HarnessTeam.PL)
public class RateLimitProtectionImMemRepository implements RateLimitProtectionRepository {
  private final ConcurrentMap<String, RateLimitProtection> store = new ConcurrentHashMap<>();

  @Override
  public void createRateLimitProtectionDataIfNotExists(String userUuid) {
    store.putIfAbsent(userUuid, RateLimitProtection.builder().build());
  }

  @Override
  public synchronized RateLimitProtection pruneIncorrectAttemptTimes(String userUuid, Long leastAllowedTime) {
    return store.compute(userUuid, (unused, oldValue) -> {
      oldValue.getIncorrectAttemptTimestamps().removeIf(timestamp -> timestamp < leastAllowedTime);
      return oldValue;
    });
  }

  @Override
  public RateLimitProtection addIncorrectAttempt(String userUuid, Long time) {
    return store.compute(userUuid, updateIncorrectAttempts(time));
  }

  @Override
  public long getAndUpdateLastEmailSentToUserAt(String userUuid, Long newTimestamp) {
    AtomicLong result = new AtomicLong();
    store.compute(userUuid, (unused, oldValue) -> {
      RateLimitProtection newValue = oldValue.toBuilder().lastNotificationSentToUserAt(newTimestamp).build();
      result.set(oldValue.getLastNotificationSentToUserAt());
      return newValue;
    });
    return result.get();
  }

  @Override
  public long getAndUpdateLastEmailSentToSecOpsAt(String userUuid, Long newTimestamp) {
    AtomicLong result = new AtomicLong();
    store.compute(userUuid, (unused, oldValue) -> {
      RateLimitProtection newValue = oldValue.toBuilder().lastNotificationSentToSecOpsAt(newTimestamp).build();
      result.set(oldValue.getLastNotificationSentToSecOpsAt());
      return newValue;
    });
    return result.get();
  }

  private BiFunction<String, RateLimitProtection, RateLimitProtection> updateIncorrectAttempts(Long time) {
    return (unusedUuid, oldValue) -> {
      List<Long> newIncorrectAttemptTimestamps = new ArrayList<>(oldValue.getIncorrectAttemptTimestamps());
      newIncorrectAttemptTimestamps.add(time);

      int newTotalIncorrectAttempts = oldValue.getTotalIncorrectAttempts() + 1;

      return oldValue.toBuilder()
          .totalIncorrectAttempts(newTotalIncorrectAttempts)
          .incorrectAttemptTimestamps(newIncorrectAttemptTimestamps)
          .build();
    };
  }
}
