package software.wings.delegatetasks;

import static software.wings.service.impl.appdynamics.AppdynamicsDataCollectionTaskResult.Builder.aAppdynamicsDataCollectionTaskResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask;
import software.wings.collect.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionTaskResult;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionTaskResult.AppdynamicsDataCollectionTaskStatus;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;

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
public class AppdynamicsDataCollectionTask extends AbstractDelegateRunnableTask<AppdynamicsDataCollectionTaskResult> {
  private static final Logger logger = LoggerFactory.getLogger(AppdynamicsDataCollectionTask.class);
  private static final int DURATION_TO_ASK_MINUTES = 5;
  private final Object lockObject = new Object();
  private final AtomicBoolean completed = new AtomicBoolean(false);

  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;

  @Inject private AppdynamicsMetricStoreService metricStoreService;

  public AppdynamicsDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<AppdynamicsDataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AppdynamicsDataCollectionTaskResult run(Object[] parameters) {
    final AppdynamicsDataCollectionInfo dataCollectionInfo = (AppdynamicsDataCollectionInfo) parameters[0];
    logger.info("metric collection - dataCollectionInfo: {}" + dataCollectionInfo);
    final ScheduledExecutorService collectionService = scheduleMetricDataCollection(dataCollectionInfo);
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    scheduledExecutorService.schedule(() -> {
      try {
        logger.info("metric collection finished for " + dataCollectionInfo);
        collectionService.shutdown();
        collectionService.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        collectionService.shutdown();
      }

      completed.set(true);
      synchronized (lockObject) {
        lockObject.notifyAll();
      }
    }, dataCollectionInfo.getCollectionTime() + 1, TimeUnit.MINUTES);
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
    return aAppdynamicsDataCollectionTaskResult().withStatus(AppdynamicsDataCollectionTaskStatus.SUCCESS).build();
  }

  private ScheduledExecutorService scheduleMetricDataCollection(AppdynamicsDataCollectionInfo dataCollectionInfo) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        new AppdynamicsMetricCollector(appdynamicsDelegateService, dataCollectionInfo, metricStoreService), 0, 1,
        TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private static class AppdynamicsMetricCollector implements Runnable {
    private final AppdynamicsDelegateService delegateService;
    private final AppdynamicsDataCollectionInfo dataCollectionInfo;
    private final AppdynamicsMetricStoreService metricStoreService;

    private AppdynamicsMetricCollector(AppdynamicsDelegateService delegateService,
        AppdynamicsDataCollectionInfo dataCollectionInfo, AppdynamicsMetricStoreService metricStoreService) {
      this.delegateService = delegateService;
      this.dataCollectionInfo = dataCollectionInfo;
      this.metricStoreService = metricStoreService;
    }

    @Override
    public void run() {
      if (dataCollectionInfo.getCollectionTime() <= 0) {
        return;
      }
      try {
        final AppDynamicsConfig appDynamicsConfig = dataCollectionInfo.getAppDynamicsConfig();
        final long appId = dataCollectionInfo.getAppId();
        final long tierId = dataCollectionInfo.getTierId();
        final List<AppdynamicsMetric> tierMetrics = delegateService.getTierBTMetrics(appDynamicsConfig, appId, tierId);

        final List<AppdynamicsMetricData> metricsData = new ArrayList<>();
        for (AppdynamicsMetric appdynamicsMetric : tierMetrics) {
          metricsData.addAll(delegateService.getTierBTMetricData(
              appDynamicsConfig, appId, tierId, appdynamicsMetric.getName(), DURATION_TO_ASK_MINUTES));
        }
        for (int i = metricsData.size() - 1; i >= 0; i--) {
          if (metricsData.get(i).getMetricName().equals("METRIC DATA NOT FOUND")) {
            metricsData.remove(i);
          }
        }
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
        logger.debug("Result: " + metricsData);
        metricStoreService.save(dataCollectionInfo.getAppDynamicsConfig().getAccountId(), dataCollectionInfo.getAppId(),
            dataCollectionInfo.getTierId(), metricsData);
      } catch (Exception e) {
        logger.error("error fetcing appdynamis metrics", e);
      }
    }
  }
}
