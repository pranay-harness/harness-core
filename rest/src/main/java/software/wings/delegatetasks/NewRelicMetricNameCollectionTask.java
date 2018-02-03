package software.wings.delegatetasks;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.impl.newrelic.NewRelicMetricData;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by sriram_parthasarathy on 12/13/17.
 */
public class NewRelicMetricNameCollectionTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(NewRelicMetricNameCollectionTask.class);

  private static final int METRIC_DATA_QUERY_BATCH_SIZE = 50;
  private static final int RETRY = 3;

  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private MetricDataStoreService metricStoreService;

  private Collection<NewRelicMetric> metrics;
  private NewRelicDataCollectionInfo dataCollectionInfo;
  private List<NewRelicApplicationInstance> instances;

  public NewRelicMetricNameCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  private List<Collection<String>> batchMetricsToCollect() {
    List<Collection<String>> rv = new ArrayList<>();

    List<String> batchedMetrics = new ArrayList<>();
    for (NewRelicMetric metric : getMetrics()) {
      batchedMetrics.add(metric.getName());

      if (batchedMetrics.size() == METRIC_DATA_QUERY_BATCH_SIZE) {
        rv.add(batchedMetrics);
        batchedMetrics = new ArrayList<>();
      }
    }

    if (!batchedMetrics.isEmpty()) {
      rv.add(batchedMetrics);
    }

    return rv;
  }

  private Collection<NewRelicMetric> getMetricsWithDataIn24Hrs() throws IOException {
    Map<String, NewRelicMetric> webTransactionMetrics = new HashMap<>();
    for (NewRelicMetric metric : getMetrics()) {
      webTransactionMetrics.put(metric.getName(), metric);
    }
    List<Collection<String>> metricBatches = batchMetricsToCollect();
    for (Collection<String> metricNames : metricBatches) {
      Set<String> metricsWithNoData = getMetricsWithNoData(metricNames);
      for (String metricName : metricsWithNoData) {
        webTransactionMetrics.remove(metricName);
      }
    }
    return webTransactionMetrics.values();
  }

  private Set<String> getMetricsWithNoData(Collection<String> metricNames) throws IOException {
    final long currentTime = System.currentTimeMillis();
    List<Callable<NewRelicMetricData>> metricDataCallabels = new ArrayList<>();
    Set<String> metricsWithNoData = Sets.newHashSet(metricNames);
    for (NewRelicApplicationInstance node : instances) {
      // find and remove metrics which have no data in last 24 hours
      metricDataCallabels.add(
          ()
              -> newRelicDelegateService.getMetricData(dataCollectionInfo.getNewRelicConfig(),
                  dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), node.getId(),
                  metricNames, currentTime - TimeUnit.DAYS.toMillis(1), currentTime));
    }

    List<Optional<NewRelicMetricData>> metricDatas = executeParrallel(metricDataCallabels);
    for (Optional<NewRelicMetricData> metricData : metricDatas) {
      if (!metricData.isPresent()) {
        throw new WingsException("Unable to get NewRelic metric data");
      }
      metricsWithNoData.removeAll(metricData.get().getMetrics_found());
      if (metricsWithNoData.isEmpty()) {
        break;
      }
    }
    return metricsWithNoData;
  }

  protected void setService(
      NewRelicDelegateService newRelicDelegateService, MetricDataStoreService metricStoreService) {
    this.newRelicDelegateService = newRelicDelegateService;
    this.metricStoreService = metricStoreService;
  }

  @Override
  public NotifyResponseData run(Object[] parameters) {
    dataCollectionInfo = (NewRelicDataCollectionInfo) parameters[0];

    try {
      metrics = newRelicDelegateService.getMetricsNameToCollect(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId());
      instances = newRelicDelegateService.getApplicationInstances(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId());

      metrics = getMetricsWithDataIn24Hrs();
      NewRelicMetricNames newRelicMetricNames =
          NewRelicMetricNames.builder()
              .metrics(new ArrayList(getMetrics()))
              .newRelicAppId(String.valueOf(dataCollectionInfo.getNewRelicAppId()))
              .newRelicConfigId(String.valueOf(dataCollectionInfo.getSettingAttributeId()))
              .build();

      newRelicMetricNames.setAppId(getAppId());

      int attempt = 0;
      while (attempt < RETRY) {
        if (metricStoreService.saveNewRelicMetricNames(getAccountId(), newRelicMetricNames)) {
          break;
        }
        attempt++;
      }

      if (attempt == RETRY) {
        throw new WingsException("Could not save new relic metric names to server");
      }

      logger.info("total available metrics for new relic " + getMetrics().size());
    } catch (Exception e) {
      logger.error("Unable to fetch NewRelic metrics {}", dataCollectionInfo.toString(), e);
      return DataCollectionTaskResult.builder()
          .status(DataCollectionTaskStatus.FAILURE)
          .stateType(StateType.NEW_RELIC)
          .errorMessage("Could not get metric names : " + e.getMessage() + " for config " + dataCollectionInfo)
          .build();
    }

    return DataCollectionTaskResult.builder().status(DataCollectionTaskStatus.SUCCESS).build();
  }

  public Collection<NewRelicMetric> getMetrics() {
    return metrics;
  }
}
