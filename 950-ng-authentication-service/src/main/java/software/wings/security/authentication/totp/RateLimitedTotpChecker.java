/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.totp;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.impl.model.RateLimit;

import software.wings.security.authentication.TotpChecker;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.LongSupplier;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class RateLimitedTotpChecker<T extends RateLimitedTotpChecker.Request> extends SimpleTotpChecker<T> {
  private static final Duration MIN_TIME_BETWEEN_TWO_EMAILS = Duration.ofHours(1);

  private final TotpChecker<? super T> totpChecker;
  private final RateLimitProtectionRepository repository;
  private final RateLimit rateLimit;
  private final NotificationService notificationService;
  private final Clock clock;
  private final int incorrectAttemptsUntilSecopsNotified;

  @Override
  public boolean check(T request) {
    String userUuid = request.getUserUuid();
    String userEmail = request.getUserEmail();
    log.debug("User with UUID {} checking TOTP", userUuid);

    long leastAllowedTime = clock.millis() - rateLimit.getDurationUnit().toMillis(rateLimit.getDuration());
    repository.createRateLimitProtectionDataIfNotExists(userUuid);
    RateLimitProtection rateLimitProtection = repository.pruneIncorrectAttemptTimes(userUuid, leastAllowedTime);

    if (attemptCountInsideWindow(rateLimitProtection) >= rateLimit.getCount()) {
      sendNotificationToUserRateLimited(userUuid, userEmail, rateLimitProtection);
      throw new RateLimitExceededException(userUuid);
    }

    // the actual checking
    boolean challengeSuccess = totpChecker.check(request);

    if (!challengeSuccess) {
      rateLimitProtection = repository.addIncorrectAttempt(userUuid, clock.millis());
      if (rateLimitProtection.getTotalIncorrectAttempts() % incorrectAttemptsUntilSecopsNotified == 0) {
        sendEmailToSecOpsRateLimited(userUuid, rateLimitProtection);
      }
    }

    return challengeSuccess;
  }

  private int attemptCountInsideWindow(RateLimitProtection rateLimitProtection) {
    return rateLimitProtection.getIncorrectAttemptTimestamps().size();
  }

  private void sendEmailToSecOpsRateLimited(String userUuid, RateLimitProtection rateLimitProtection) {
    sendNotificationRateLimited(rateLimitProtection.getLastNotificationSentToSecOpsAt(),
        ()
            -> repository.getAndUpdateLastEmailSentToSecOpsAt(userUuid, clock.millis()),
        () -> notificationService.notifySecOps(userUuid));
  }

  private void sendNotificationToUserRateLimited(
      String userUuid, String userEmail, RateLimitProtection rateLimitProtection) {
    sendNotificationRateLimited(rateLimitProtection.getLastNotificationSentToUserAt(),
        ()
            -> repository.getAndUpdateLastEmailSentToUserAt(userUuid, clock.millis()),
        () -> notificationService.notifyUser(userEmail));
  }

  /**
   * Used both for sending emails to SecOps & to users
   *
   * This is complicated because in the first iteration we do not use distributed locking
   * mechanism. So we are relying of atomic operations in which {@link RateLimitProtectionRepository} has to implement.
   */
  private void sendNotificationRateLimited(long lastNotificationSentAtMillis,
      LongSupplier getAndUpdateLastEmailSentAtSupplier, Runnable notificationSendAction) {
    Instant lastNotificationSentAt = Instant.ofEpochMilli(lastNotificationSentAtMillis);

    Duration timeSinceLastNotification = Duration.between(lastNotificationSentAt, clock.instant());
    if (timeSinceLastNotification.toMillis() >= MIN_TIME_BETWEEN_TWO_EMAILS.toMillis()) {
      // multiple threads from different jvms can get to this point, but only the first one to execute this operation
      // will get the value which is more than MIN_TIME_BETWEEN_TWO_EMAILS from current timestamp. the rest will get
      // times which are a couple of milliseconds greater then current timestamp
      lastNotificationSentAtMillis = getAndUpdateLastEmailSentAtSupplier.getAsLong(); // atomic

      lastNotificationSentAt = Instant.ofEpochMilli(lastNotificationSentAtMillis);

      timeSinceLastNotification = Duration.between(lastNotificationSentAt, clock.instant());
      if (timeSinceLastNotification.toMillis() >= MIN_TIME_BETWEEN_TWO_EMAILS.toMillis()) {
        // only one thread should be able to enter here, the first one that called getAndUpdateLastEmailSentAtSupplier
        // that thread will send the notification
        notificationSendAction.run();
      }
    }
  }

  @Getter
  @EqualsAndHashCode(callSuper = true)
  public static class Request extends SimpleTotpChecker.Request {
    private final String userUuid;
    private final String userEmail;

    public Request(String secret, int code, String userUuid, String userEmail) {
      super(secret, code);
      this.userUuid = userUuid;
      this.userEmail = userEmail;
    }
  }
}
