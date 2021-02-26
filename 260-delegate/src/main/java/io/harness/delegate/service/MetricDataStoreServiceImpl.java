package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.managerclient.VerificationServiceClient;
import io.harness.rest.RestResponse;

import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(Module._420_DELEGATE_AGENT)
public class MetricDataStoreServiceImpl implements MetricDataStoreService {
  @Inject private VerificationServiceClient verificationClient;
  @Inject private TimeLimiter timeLimiter;

  @Override
  public boolean saveNewRelicMetrics(String accountId, String applicationId, String stateExecutionId,
      String delegateTaskId, List<NewRelicMetricDataRecord> metricData) {
    if (metricData.isEmpty()) {
      return true;
    }

    try {
      RestResponse<Boolean> restResponse =
          timeLimiter.callWithTimeout(()
                                          -> execute(verificationClient.saveTimeSeriesMetrics(
                                              accountId, applicationId, stateExecutionId, delegateTaskId, metricData)),
              15, TimeUnit.SECONDS, true);
      if (restResponse == null) {
        return false;
      }

      return restResponse.getResource();
    } catch (Exception e) {
      log.error("error saving new apm metrics", e);
      return false;
    }
  }
}
