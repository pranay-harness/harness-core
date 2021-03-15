package software.wings.verification.prometheus;

import software.wings.metrics.MetricType;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.verification.CVConfiguration;
import software.wings.verification.ServiceGuardThroughputToErrorsMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.Attributes;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PrometheusCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Metrics To Monitor") private List<TimeSeries> timeSeriesToAnalyze;

  @Override
  @JsonIgnore
  public List<ServiceGuardThroughputToErrorsMap> getThroughputToErrors() {
    Preconditions.checkNotNull(timeSeriesToAnalyze);
    List<ServiceGuardThroughputToErrorsMap> serviceGuardThroughputToErrorsMaps = new ArrayList<>();
    Map<String, ServiceGuardThroughputToErrorsMap> txnToThroughputMap = new HashMap<>();
    timeSeriesToAnalyze.stream()
        .filter(timeSeries -> MetricType.THROUGHPUT == MetricType.valueOf(timeSeries.getMetricType()))
        .forEach(timeSeries -> {
          final ServiceGuardThroughputToErrorsMap throughputToErrorsMap =
              ServiceGuardThroughputToErrorsMap.builder()
                  .txnName(timeSeries.getTxnName())
                  .throughputMetric(timeSeries.getMetricName())
                  .errorMetrics(Lists.newArrayList())
                  .build();
          serviceGuardThroughputToErrorsMaps.add(throughputToErrorsMap);
          txnToThroughputMap.put(timeSeries.getTxnName(), throughputToErrorsMap);
        });

    timeSeriesToAnalyze.stream()
        .filter(timeSeries -> MetricType.ERROR == MetricType.valueOf(timeSeries.getMetricType()))
        .forEach(timeSeries
            -> txnToThroughputMap.get(timeSeries.getTxnName()).getErrorMetrics().add(timeSeries.getMetricName()));

    return serviceGuardThroughputToErrorsMaps;
  }

  @Override
  public CVConfiguration deepCopy() {
    PrometheusCVServiceConfiguration clonedConfig = new PrometheusCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setTimeSeriesToAnalyze(getTimeSeriesToAnalyze());
    return clonedConfig;
  }
}
