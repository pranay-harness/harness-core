package software.wings.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.verification.VerificationServiceClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by raghu on 5/19/17.
 */
@Singleton
public class MetricDataStoreServiceImpl implements MetricDataStoreService {
  private static final Logger logger = LoggerFactory.getLogger(MetricDataStoreServiceImpl.class);

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
