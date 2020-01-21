package software.wings.delegatetasks.cv;

import static software.wings.common.VerificationConstants.DEFAULT_GROUP_NAME;
import static software.wings.common.VerificationConstants.INSTANA_DOCKER_PLUGIN;
import static software.wings.common.VerificationConstants.VERIFICATION_HOST_PLACEHOLDERV2;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import software.wings.service.impl.analysis.MetricElement;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
import software.wings.service.impl.instana.InstanaInfraMetricRequest;
import software.wings.service.impl.instana.InstanaInfraMetrics;
import software.wings.service.impl.instana.InstanaTimeFrame;
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
  @Inject InstanaDelegateService instanaDelegateService;
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
    Duration duration = Duration.between(dataCollectionInfo.getStartTime(), dataCollectionInfo.getEndTime());
    Preconditions.checkArgument(hostBatch.size() == 1, "size can not be greater than 1");
    InstanaTimeFrame instanaTimeFrame = InstanaTimeFrame.builder()
                                            .to(dataCollectionInfo.getEndTime().toEpochMilli() - 1)
                                            .windowSize(TimeUnit.SECONDS.toMillis(duration.getSeconds()) - 1)
                                            .build();

    InstanaInfraMetricRequest instanaInfraMetricRequest =
        InstanaInfraMetricRequest.builder()
            .metrics(dataCollectionInfo.getMetrics())
            .plugin(INSTANA_DOCKER_PLUGIN)
            .query(
                dataCollectionInfo.getQuery().replace(VERIFICATION_HOST_PLACEHOLDERV2, "\"" + hostBatch.get(0)) + "\"")
            .rollup(60)
            .timeframe(instanaTimeFrame)
            .build();
    InstanaInfraMetrics instanaInfraMetrics = instanaDelegateService.getInfraMetrics(
        dataCollectionInfo.getInstanaConfig(), dataCollectionInfo.getEncryptedDataDetails(), instanaInfraMetricRequest,
        dataCollectionExecutionContext.createApiCallLog());

    return getMetricElements(instanaInfraMetrics, hostBatch.get(0));
  }

  private List<MetricElement> getMetricElements(InstanaInfraMetrics instanaInfraMetrics, String host) {
    if (instanaInfraMetrics.getItems().isEmpty()) {
      return Collections.emptyList();
    }
    Map<Long, MetricElement> timestampToMetricMap = new HashMap<>();
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
          metricName.replace(".", "_"), metricValue.get(1).doubleValue()); // TODO: find a better way to handle this.
      timestampToMetricMap.put(timestamp, metricElement);
    }));
    return new ArrayList<>(timestampToMetricMap.values());
  }

  @Override
  public List<MetricElement> fetchMetrics() throws DataCollectionException {
    throw new UnsupportedOperationException("Not supported");
  }
}
