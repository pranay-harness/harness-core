package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;
import static software.wings.common.VerificationConstants.MAX_RETRIES;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import io.harness.verification.VerificationServiceClient;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import software.wings.delegatetasks.DelegateCVTaskService;
import software.wings.service.impl.analysis.DataCollectionTaskResult;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
@Slf4j
public class DelegateCVTaskServiceImpl implements DelegateCVTaskService {
  private static final int TIMEOUT_DURATION_SEC = 5;
  @VisibleForTesting static Duration DELAY = Duration.ofSeconds(1);
  @Inject private VerificationServiceClient verificationClient;
  @Inject private TimeLimiter timeLimiter;
  @Override
  public void updateCVTaskStatus(String accountId, String cvTaskId, DataCollectionTaskResult dataCollectionTaskResult)
      throws TimeoutException {
    try {
      RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                                            .handle(Exception.class)
                                            .withDelay(DELAY)
                                            .withMaxRetries(MAX_RETRIES)
                                            .onFailedAttempt(event
                                                -> logger.info("[Retrying]: Failed updating task status attempt: {}",
                                                    event.getAttemptCount(), event.getLastFailure()))
                                            .onFailure(event
                                                -> logger.error("[Failed]: Failed updating task status attempt: {}",
                                                    event.getAttemptCount(), event.getFailure()));
      Failsafe.with(retryPolicy)
          .run(()
                   -> timeLimiter.callWithTimeout(()
                                                      -> execute(verificationClient.updateCVTaskStatus(
                                                          accountId, cvTaskId, dataCollectionTaskResult)),
                       TIMEOUT_DURATION_SEC, TimeUnit.SECONDS, true));
    } catch (UncheckedTimeoutException e) {
      throw new TimeoutException("Timeout of " + TIMEOUT_DURATION_SEC + " sec and " + MAX_RETRIES
          + " retries exceeded while updating CVTask status");
    }
  }
}
