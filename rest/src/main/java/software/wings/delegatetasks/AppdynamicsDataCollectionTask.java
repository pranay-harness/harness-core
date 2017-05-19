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

  public AppdynamicsDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<AppdynamicsDataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AppdynamicsDataCollectionTaskResult run(Object[] parameters) {
    final List<AppdynamicsDataCollectionInfo> dataCollectionInfoList =
        (List<AppdynamicsDataCollectionInfo>) parameters[0];
    final int maxCollectionTime = getMaxCollectionTime(dataCollectionInfoList);
    final ScheduledExecutorService collectionService = scheduleMetricDataCollection(dataCollectionInfoList);
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    scheduledExecutorService.schedule(() -> {
      try {
        logger.info("metric collection finished for " + dataCollectionInfoList);
        collectionService.shutdown();
        collectionService.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        collectionService.shutdown();
      }

      completed.set(true);
      synchronized (lockObject) {
        lockObject.notifyAll();
      }
    }, maxCollectionTime + 1, TimeUnit.MINUTES);
    logger.info("going to collect appdynamics data for " + dataCollectionInfoList);

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

  private int getMaxCollectionTime(List<AppdynamicsDataCollectionInfo> dataCollectionInfoList) {
    int maxCollectionTime = 0;
    for (AppdynamicsDataCollectionInfo dataCollectionInfo : dataCollectionInfoList) {
      if (maxCollectionTime < dataCollectionInfo.getCollectionTime()) {
        maxCollectionTime = dataCollectionInfo.getCollectionTime();
      }
    }

    return maxCollectionTime;
  }

  private ScheduledExecutorService scheduleMetricDataCollection(
      List<AppdynamicsDataCollectionInfo> dataCollectionInfoList) {
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(dataCollectionInfoList.size());
    for (AppdynamicsDataCollectionInfo dataCollectionInfo : dataCollectionInfoList) {
      scheduledExecutorService.scheduleAtFixedRate(
          new AppdynamicsMetricCollector(appdynamicsDelegateService, dataCollectionInfo), 0, 1, TimeUnit.MINUTES);
    }
    return scheduledExecutorService;
  }

  private static class AppdynamicsMetricCollector implements Runnable {
    private final AppdynamicsDelegateService delegateService;
    private final AppdynamicsDataCollectionInfo dataCollectionInfo;

    private AppdynamicsMetricCollector(
        AppdynamicsDelegateService delegateService, AppdynamicsDataCollectionInfo dataCollectionInfo) {
      this.delegateService = delegateService;
      this.dataCollectionInfo = dataCollectionInfo;
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
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
        logger.info("Result: " + metricsData);
      } catch (Exception e) {
        logger.error("error fetcing appdynamis metrics", e);
      }
    }
  }
}
