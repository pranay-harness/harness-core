package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.harness.verification.VerificationServiceClient;
import software.wings.delegatetasks.DelegateCVTaskService;
import software.wings.service.impl.analysis.DataCollectionTaskResult;

import java.util.concurrent.TimeUnit;

public class DelegateCVTaskServiceImpl implements DelegateCVTaskService {
  @Inject private VerificationServiceClient verificationClient;
  @Inject private TimeLimiter timeLimiter;
  @Override
  public void updateCVTaskStatus(String accountId, String cvTaskId, DataCollectionTaskResult dataCollectionTaskResult) {
    try {
      timeLimiter.callWithTimeout(
          ()
              -> execute(verificationClient.updateCVTaskStatus(accountId, cvTaskId, dataCollectionTaskResult)),
          15, TimeUnit.SECONDS, true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
