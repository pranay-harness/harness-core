package software.wings.delegatetasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask;
import software.wings.metrics.appdynamics.AppdynamicsConstants;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rsingh on 5/18/17.
 */
public class AppdynamicsDataCollectionTask extends AbstractDelegateRunnableTask<DataCollectionTaskResult> {
  private static final Logger logger = LoggerFactory.getLogger(AppdynamicsDataCollectionTask.class);
  private static final int DURATION_TO_ASK_MINUTES = 5;
  private final Object lockObject = new Object();
  private final AtomicBoolean completed = new AtomicBoolean(false);
  private ScheduledExecutorService collectionService;

  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;

  @Inject private MetricDataStoreService metricStoreService;

  public AppdynamicsDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public DataCollectionTaskResult run(Object[] parameters) {
    final AppdynamicsDataCollectionInfo dataCollectionInfo = (AppdynamicsDataCollectionInfo) parameters[0];
    logger.info("metric collection - dataCollectionInfo: {}" + dataCollectionInfo);
    collectionService = scheduleMetricDataCollection(dataCollectionInfo);
    logger.info("going to collect appdynamics data for " + dataCollectionInfo);

    synchronized (lockObject) {
      while (!completed.get()) {
        try {
          lockObject.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.APP_DYNAMICS)
        .build();
  }

  private ScheduledExecutorService scheduleMetricDataCollection(AppdynamicsDataCollectionInfo dataCollectionInfo) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        new AppdynamicsMetricCollector(dataCollectionInfo), 0, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private void shutDownCollection() {
    collectionService.shutdown();
    completed.set(true);
    synchronized (lockObject) {
      lockObject.notifyAll();
    }
  }

  private class AppdynamicsMetricCollector implements Runnable {
    private final AppdynamicsDataCollectionInfo dataCollectionInfo;

    private AppdynamicsMetricCollector(AppdynamicsDataCollectionInfo dataCollectionInfo) {
      this.dataCollectionInfo = dataCollectionInfo;
    }

    @Override
    public void run() {
      if (dataCollectionInfo.getCollectionTime() <= 0) {
        shutDownCollection();
        return;
      }
      try {
        final AppDynamicsConfig appDynamicsConfig = dataCollectionInfo.getAppDynamicsConfig();
        final long appId = dataCollectionInfo.getAppId();
        final long tierId = dataCollectionInfo.getTierId();
        final List<AppdynamicsMetric> tierMetrics =
            appdynamicsDelegateService.getTierBTMetrics(appDynamicsConfig, appId, tierId);

        final List<AppdynamicsMetricData> metricsData = new ArrayList<>();
        for (AppdynamicsMetric appdynamicsMetric : tierMetrics) {
          metricsData.addAll(appdynamicsDelegateService.getTierBTMetricData(
              appDynamicsConfig, appId, tierId, appdynamicsMetric.getName(), DURATION_TO_ASK_MINUTES));
        }
        for (int i = metricsData.size() - 1; i >= 0; i--) {
          String metricName = metricsData.get(i).getMetricName();
          if (metricName.contains("|")) {
            metricName = metricName.substring(metricName.lastIndexOf("|") + 1);
          }
          if (!AppdynamicsConstants.METRICS_TO_TRACK.contains(metricName)) {
            metricsData.remove(i);
            if (!metricName.equals("METRIC DATA NOT FOUND")) {
              logger.debug("metric with unexpected name found: " + metricName);
            }
          }
        }
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
        logger.debug("Result: " + metricsData);
        metricStoreService.saveAppDynamicsMetrics(dataCollectionInfo.getAppDynamicsConfig().getAccountId(),
            dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
            dataCollectionInfo.getAppId(), dataCollectionInfo.getTierId(), metricsData);
      } catch (Exception e) {
        logger.error("error fetching appdynamics metrics", e);
      }
    }
  }
}
