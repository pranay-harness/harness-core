package software.wings.service.intfc.prometheus;

import software.wings.beans.PrometheusConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.prometheus.PrometheusMetricDataResponse;

/**
 * Created by rsingh on 01/29/18.
 */
public interface PrometheusDelegateService {
  @DelegateTaskType(TaskType.PROMETHEUS_METRIC_DATA_PER_HOST)
  PrometheusMetricDataResponse fetchMetricData(
      PrometheusConfig prometheusConfig, String url, ThirdPartyApiCallLog apiCallLog);
}
