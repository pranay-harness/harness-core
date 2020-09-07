package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.managerclient.VerificationServiceClient;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by raghu on 5/19/17.
 */
@Singleton
@Slf4j
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
      logger.error("error saving new apm metrics", e);
      return false;
    }
  }
}
