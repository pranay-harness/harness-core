package software.wings.delegatetasks.cv;

import static software.wings.common.VerificationConstants.DEFAULT_GROUP_NAME;
import static software.wings.common.VerificationConstants.INSTANA_DOCKER_PLUGIN;
import static software.wings.common.VerificationConstants.VERIFICATION_HOST_PLACEHOLDERV2;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import software.wings.service.impl.analysis.MetricElement;
import software.wings.service.impl.instana.InstanaAnalyzeMetricRequest;
import software.wings.service.impl.instana.InstanaAnalyzeMetrics;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
import software.wings.service.impl.instana.InstanaInfraMetricRequest;
import software.wings.service.impl.instana.InstanaInfraMetrics;
import software.wings.service.impl.instana.InstanaMetricTemplate;
import software.wings.service.impl.instana.InstanaTagFilter;
import software.wings.service.impl.instana.InstanaTimeFrame;
import software.wings.service.impl.instana.InstanaUtils;
import software.wings.service.intfc.instana.InstanaDelegateService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InstanaDataCollector implements MetricsDataCollector<InstanaDataCollectionInfo> {
  public static final String INFRASTRUCTURE = "Infrastructure";
  @Inject private InstanaDelegateService instanaDelegateService;
  private InstanaDataCollectionInfo dataCollectionInfo;
  private DataCollectionExecutionContext dataCollectionExecutionContext;

  @Override
  public void init(DataCollectionExecutionContext dataCollectionExecutionContext,
      InstanaDataCollectionInfo dataCollectionInfo) throws DataCollectionException {
    this.dataCollectionInfo = dataCollectionInfo;
    this.dataCollectionExecutionContext = dataCollectionExecutionContext;
  }

  @Override
  public int getHostBatchSize() {
    return 1;
  }
  @Override
  public List<MetricElement> fetchMetrics(List<String> hostBatch) throws DataCollectionException {
    Preconditions.checkArgument(hostBatch.size() == 1);
    Preconditions.checkArgument(dataCollectionInfo.getQuery().contains(VERIFICATION_HOST_PLACEHOLDERV2),
        "Query should contain %s", VERIFICATION_HOST_PLACEHOLDERV2);
    Preconditions.checkArgument(hostBatch.size() == 1, "size can not be greater than 1");
    InstanaInfraMetricRequest instanaInfraMetricRequest =
        InstanaInfraMetricRequest.builder()
            .metrics(dataCollectionInfo.getMetrics())
            .plugin(INSTANA_DOCKER_PLUGIN)
            .query(
                dataCollectionInfo.getQuery().replace(VERIFICATION_HOST_PLACEHOLDERV2, "\"" + hostBatch.get(0)) + "\"")
            .rollup(60)
            .timeframe(getTimeframeForInfraMetric())
            .build();
    InstanaInfraMetrics instanaInfraMetrics = instanaDelegateService.getInfraMetrics(
        dataCollectionInfo.getInstanaConfig(), dataCollectionInfo.getEncryptedDataDetails(), instanaInfraMetricRequest,
        dataCollectionExecutionContext.createApiCallLog());
    List<InstanaAnalyzeMetricRequest.Metric> metrics = new ArrayList<>();
    InstanaUtils.getApplicationMetricTemplateMap().forEach(
        (metricName, instanaMetricTemplate)
            -> metrics.add(InstanaAnalyzeMetricRequest.Metric.builder()
                               .metric(instanaMetricTemplate.getMetricName())
                               .aggregation(instanaMetricTemplate.getAggregation())
                               .granularity(60)
                               .build()));
    InstanaAnalyzeMetricRequest.Group group =
        InstanaAnalyzeMetricRequest.Group.builder().groupByTag("trace.name").build();
    List<InstanaTagFilter> tagFilters = new ArrayList<>(dataCollectionInfo.getTagFilters());
    tagFilters.add(InstanaTagFilter.builder()
                       .name(dataCollectionInfo.getHostTagFilter())
                       .value(hostBatch.get(0))
                       .operator(InstanaTagFilter.Operator.EQUALS)
                       .build());
    InstanaAnalyzeMetricRequest instanaAnalyzeMetricRequest = InstanaAnalyzeMetricRequest.builder()
                                                                  .metrics(metrics)
                                                                  .group(group)
                                                                  .tagFilters(tagFilters)
                                                                  .timeFrame(getTimeframeForTraceMetric())
                                                                  .build();

    InstanaAnalyzeMetrics instanaAnalyzeMetrics = instanaDelegateService.getInstanaTraceMetrics(
        dataCollectionInfo.getInstanaConfig(), dataCollectionInfo.getEncryptedDataDetails(),
        instanaAnalyzeMetricRequest, dataCollectionExecutionContext.createApiCallLog());
    List<MetricElement> metricElements = new ArrayList<>();
    metricElements.addAll(getMetricElements(instanaInfraMetrics, hostBatch.get(0)));
    metricElements.addAll(getMetricElements(instanaAnalyzeMetrics, hostBatch.get(0)));
    return metricElements;
  }

  private InstanaTimeFrame getTimeframeForTraceMetric() {
    Duration duration = Duration.between(dataCollectionInfo.getStartTime(), dataCollectionInfo.getEndTime());
    return InstanaTimeFrame.builder()
        .to(dataCollectionInfo.getEndTime().toEpochMilli())
        .windowSize(TimeUnit.SECONDS.toMillis(duration.getSeconds()))
        .build();
  }
  private InstanaTimeFrame getTimeframeForInfraMetric() {
    // unfortunately both APIs need different way of creating timeframe.
    Duration duration = Duration.between(dataCollectionInfo.getStartTime(), dataCollectionInfo.getEndTime());
    return InstanaTimeFrame.builder()
        .to(dataCollectionInfo.getEndTime().toEpochMilli() - 1)
        .windowSize(TimeUnit.SECONDS.toMillis(duration.getSeconds() - 1))
        .build();
  }

  private List<MetricElement> getMetricElements(InstanaInfraMetrics instanaInfraMetrics, String host) {
    if (instanaInfraMetrics.getItems().isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, MetricElement> timestampToMetricMap = new HashMap<>();
    Map<String, InstanaMetricTemplate> metricTemplateMap = InstanaUtils.getInfraMetricTemplateMap();
    instanaInfraMetrics.getItems().get(0).getMetrics().forEach((metricName, values) -> values.forEach(metricValue -> {
      Long timestamp = metricValue.get(0).longValue();
      MetricElement metricElement = timestampToMetricMap.getOrDefault(timestamp,
          MetricElement.builder()
              .groupName(DEFAULT_GROUP_NAME)
              .host(host)
              .name(INFRASTRUCTURE)
              .timestamp(timestamp)
              .build());

      metricElement.getValues().put(
          metricTemplateMap.get(metricName).getDisplayName(), metricValue.get(1).doubleValue());
      timestampToMetricMap.put(timestamp, metricElement);
    }));
    return new ArrayList<>(timestampToMetricMap.values());
  }

  private List<MetricElement> getMetricElements(InstanaAnalyzeMetrics instanaAnalyzeMetrics, String host) {
    List<MetricElement> metricElements = new ArrayList<>();
    instanaAnalyzeMetrics.getItems().forEach(item -> {
      MetricElement metricElement = MetricElement.builder()
                                        .timestamp(item.getTimestamp())
                                        .groupName(DEFAULT_GROUP_NAME)
                                        .host(host)
                                        .name(item.getName())
                                        .build();
      Map<String, Double> values = new HashMap<>();
      // TODO: timeframe should be 1 min when implementing service guard for workflow value will always have one
      // element.
      Map<String, InstanaMetricTemplate> metricTemplateMap = InstanaUtils.getApplicationMetricTemplateMap();
      item.getMetrics().forEach((metricName, value) -> {
        metricName = metricName.split("\\.")[0]; // ex: latency.p99.60
        values.put(metricTemplateMap.get(metricName).getDisplayName(), value.get(0).get(1).doubleValue());
        Preconditions.checkState(value.size() <= 1, "metric size should less then 1 for metric name %s", metricName);
        metricElement.setTimestamp(value.get(0).get(0).longValue()); // this will be same for all the values.
      });
      metricElement.setValues(values);
      metricElements.add(metricElement);
    });
    return metricElements;
  }

  @Override
  public List<MetricElement> fetchMetrics() throws DataCollectionException {
    throw new UnsupportedOperationException("Not supported");
  }
}
