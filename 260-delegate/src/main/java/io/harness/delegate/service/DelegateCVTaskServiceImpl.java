package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import static software.wings.common.VerificationConstants.MAX_RETRIES;

import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.concurrent.HTimeLimiter;
import io.harness.managerclient.VerificationServiceClient;

import software.wings.delegatetasks.DelegateCVTaskService;
import software.wings.service.impl.analysis.DataCollectionTaskResult;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
@Slf4j
@TargetModule(Module._420_DELEGATE_AGENT)
public class DelegateCVTaskServiceImpl implements DelegateCVTaskService {
  private static final int TIMEOUT_DURATION_SEC = 5;
  @VisibleForTesting static Duration DELAY = ofSeconds(1);
  @Inject private VerificationServiceClient verificationClient;
  @Inject private HTimeLimiter timeLimiter;
  @Override
  public void updateCVTaskStatus(String accountId, String cvTaskId, DataCollectionTaskResult dataCollectionTaskResult)
      throws TimeoutException {
    try {
      RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
                                            .handle(Exception.class)
                                            .withDelay(DELAY)
                                            .withMaxRetries(MAX_RETRIES)
                                            .onFailedAttempt(event
                                                -> log.info("[Retrying]: Failed updating task status attempt: {}",
                                                    event.getAttemptCount(), event.getLastFailure()))
                                            .onFailure(event
                                                -> log.error("[Failed]: Failed updating task status attempt: {}",
                                                    event.getAttemptCount(), event.getFailure()));
      Failsafe.with(retryPolicy)
          .run(()
                   -> timeLimiter.callInterruptible(ofSeconds(TIMEOUT_DURATION_SEC),
                       ()
                           -> execute(
                               verificationClient.updateCVTaskStatus(accountId, cvTaskId, dataCollectionTaskResult))));
    } catch (UncheckedTimeoutException e) {
      throw new TimeoutException("Timeout of " + TIMEOUT_DURATION_SEC + " sec and " + MAX_RETRIES
          + " retries exceeded while updating CVTask status");
    }
  }
}
