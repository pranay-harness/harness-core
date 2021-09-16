package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.concurrent.HTimeLimiter;
import io.harness.managerclient.VerificationServiceClient;
import io.harness.rest.RestResponse;

import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class MetricDataStoreServiceImpl implements MetricDataStoreService {
  @Inject private VerificationServiceClient verificationClient;
  @Inject private TimeLimiter timeLimiter;

  @Override
  public boolean saveNewRelicMetrics(String accountId, String applicationId, String stateExecutionId,
      String delegateTaskId, List<NewRelicMetricDataRecord> metricData) throws Exception {
    if (metricData.isEmpty()) {
      return true;
    }

    RestResponse<Boolean> restResponse = HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofSeconds(15),
        ()
            -> execute(verificationClient.saveTimeSeriesMetrics(
                accountId, applicationId, stateExecutionId, delegateTaskId, metricData)));
    return restResponse.getResource();
  }
}
