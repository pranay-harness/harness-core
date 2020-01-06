package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.harness.verification.VerificationServiceClient;
import software.wings.delegatetasks.DelegateCVTaskService;
import software.wings.service.impl.analysis.DataCollectionTaskResult;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DelegateCVTaskServiceImpl implements DelegateCVTaskService {
  private static final int TIMEOUT_DURATION_SEC = 15;
  @Inject private VerificationServiceClient verificationClient;
  @Inject private TimeLimiter timeLimiter;
  @Override
  public void updateCVTaskStatus(String accountId, String cvTaskId, DataCollectionTaskResult dataCollectionTaskResult)
      throws TimeoutException {
    try {
      timeLimiter.callWithTimeout(
          ()
              -> execute(verificationClient.updateCVTaskStatus(accountId, cvTaskId, dataCollectionTaskResult)),
          TIMEOUT_DURATION_SEC, TimeUnit.SECONDS, true);
    } catch (Exception e) {
      throw new TimeoutException("Timeout of " + TIMEOUT_DURATION_SEC + " sec exceeded while updating CVTask status");
    }
  }
}
