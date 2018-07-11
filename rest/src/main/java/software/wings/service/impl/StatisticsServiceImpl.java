package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.time.EpochUtil.PST_ZONE_ID;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.beans.WorkflowType.SIMPLE;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.time.EpochUtil;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats.DayStat;
import software.wings.beans.stats.ServiceInstanceStatistics;
import software.wings.beans.stats.TopConsumer;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by anubhaw on 8/15/16.
 */
@Singleton
public class StatisticsServiceImpl implements StatisticsService {
  @Inject private AppService appService;
  @Inject private NotificationService notificationService;
  @Inject private ActivityService activityService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  public DeploymentStatistics getDeploymentStatistics(String accountId, List<String> appIds, int numOfDays) {
    long fromDateEpochMilli = getEpochMilliPSTZone(numOfDays);
    DeploymentStatistics deploymentStats = new DeploymentStatistics();
    if (isEmpty(appIds)) {
      appIds = appService.getAppIdsByAccountId(accountId);
    }
    if (isEmpty(appIds)) {
      return deploymentStats;
    }
    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.obtainWorkflowExecutions(appIds, fromDateEpochMilli);
    Map<EnvironmentType, List<WorkflowExecution>> wflExecutionByEnvType =
        workflowExecutions.parallelStream().collect(groupingBy(wex -> PROD.equals(wex.getEnvType()) ? PROD : NON_PROD));

    deploymentStats.getStatsMap().put(
        PROD, getDeploymentStatisticsByEnvType(numOfDays, wflExecutionByEnvType.get(EnvironmentType.PROD)));
    deploymentStats.getStatsMap().put(
        NON_PROD, getDeploymentStatisticsByEnvType(numOfDays, wflExecutionByEnvType.get(EnvironmentType.NON_PROD)));
    deploymentStats.getStatsMap().put(
        ALL, merge(deploymentStats.getStatsMap().get(PROD), deploymentStats.getStatsMap().get(NON_PROD)));
    return deploymentStats;
  }

  @Override
  public ServiceInstanceStatistics getServiceInstanceStatistics(String accountId, List<String> appIds, int numOfDays) {
    long fromDateEpochMilli = getEpochMilliPSTZone(numOfDays);

    ServiceInstanceStatistics instanceStats = new ServiceInstanceStatistics();
    if (isEmpty(appIds)) {
      appIds = appService.getAppIdsByAccountId(accountId);
      if (isEmpty(appIds)) {
        return instanceStats;
      }
    }
    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.obtainWorkflowExecutions(appIds, fromDateEpochMilli);
    Comparator<TopConsumer> byCount = comparing(TopConsumer::getTotalCount, reverseOrder());

    List<TopConsumer> allTopConsumers = new ArrayList<>();
    getTopServicesDeployed(allTopConsumers, workflowExecutions);

    allTopConsumers = allTopConsumers.stream().sorted(byCount).collect(toList());

    Map<EnvironmentType, List<WorkflowExecution>> wflExecutionByEnvType =
        workflowExecutions.parallelStream().collect(groupingBy(wex -> PROD.equals(wex.getEnvType()) ? PROD : NON_PROD));

    List<TopConsumer> prodTopConsumers = new ArrayList<>();
    getTopServicesDeployed(prodTopConsumers, wflExecutionByEnvType.get(PROD));
    prodTopConsumers = prodTopConsumers.stream().sorted(byCount).collect(toList());

    List<TopConsumer> nonProdTopConsumers = new ArrayList<>();
    getTopServicesDeployed(nonProdTopConsumers, wflExecutionByEnvType.get(NON_PROD));

    nonProdTopConsumers = nonProdTopConsumers.stream().sorted(byCount).collect(toList());

    instanceStats.getStatsMap().put(ALL, allTopConsumers);
    instanceStats.getStatsMap().put(PROD, prodTopConsumers);
    instanceStats.getStatsMap().put(NON_PROD, nonProdTopConsumers);
    return instanceStats;
  }

  private AggregatedDayStats merge(AggregatedDayStats prodAggStats, AggregatedDayStats nonProdAggStats) {
    List<DayStat> dayStats = new ArrayList<>(prodAggStats.getDaysStats().size());

    IntStream.range(0, prodAggStats.getDaysStats().size()).forEach(idx -> {
      DayStat prod = prodAggStats.getDaysStats().get(idx);
      DayStat nonProd = nonProdAggStats.getDaysStats().get(idx);
      dayStats.add(
          new DayStat(prod.getTotalCount() + nonProd.getTotalCount(), prod.getFailedCount() + nonProd.getFailedCount(),
              prod.getInstancesCount() + nonProd.getInstancesCount(), prod.getDate()));
    });
    return new AggregatedDayStats(prodAggStats.getTotalCount() + nonProdAggStats.getTotalCount(),
        prodAggStats.getFailedCount() + nonProdAggStats.getFailedCount(),
        prodAggStats.getInstancesCount() + nonProdAggStats.getInstancesCount(), dayStats);
  }

  private AggregatedDayStats getDeploymentStatisticsByEnvType(
      int numOfDays, List<WorkflowExecution> workflowExecutions) {
    List<DayStat> dayStats = new ArrayList<>(numOfDays);

    Map<Long, List<WorkflowExecution>> wflExecutionByDate = new HashMap<>();
    if (workflowExecutions != null) {
      wflExecutionByDate = workflowExecutions.parallelStream().collect(
          groupingBy(wfl -> EpochUtil.obtainStartOfTheDayEpoch(wfl.getCreatedAt(), PST_ZONE_ID)));
    }

    int aggTotalCount = 0;
    int aggFailureCount = 0;
    int aggInstanceCount = 0;

    for (int idx = 0; idx < numOfDays; idx++) {
      int totalCount = 0;
      int failureCount = 0;
      int instanceCount = 0;

      Long timeOffset = getEpochMilliPSTZone(numOfDays - idx);
      List<WorkflowExecution> wflExecutions = wflExecutionByDate.get(timeOffset);
      if (wflExecutions != null) {
        totalCount = wflExecutions.size();
        failureCount = (int) wflExecutions.stream()
                           .map(WorkflowExecution::getStatus)
                           .filter(ExecutionStatus.negativeStatuses()::contains)
                           .count();
        for (WorkflowExecution workflowExecution : wflExecutions) {
          if ((workflowExecution.getWorkflowType() == ORCHESTRATION || workflowExecution.getWorkflowType() == SIMPLE)
              && workflowExecution.getServiceExecutionSummaries() != null) {
            instanceCount += workflowExecution.getServiceExecutionSummaries()
                                 .stream()
                                 .map(ElementExecutionSummary::getInstancesCount)
                                 .mapToInt(i -> i)
                                 .sum();
          } else if (workflowExecution.getWorkflowType() == PIPELINE && workflowExecution.getPipelineExecution() != null
              && workflowExecution.getPipelineExecution().getPipelineStageExecutions() != null) {
            for (PipelineStageExecution pipelineStageExecution :
                workflowExecution.getPipelineExecution().getPipelineStageExecutions()) {
              if (pipelineStageExecution == null || pipelineStageExecution.getWorkflowExecutions() == null) {
                continue;
              }
              instanceCount +=
                  pipelineStageExecution.getWorkflowExecutions()
                      .stream()
                      .filter(workflowExecution1 -> workflowExecution1.getServiceExecutionSummaries() != null)
                      .flatMap(workflowExecution1 -> workflowExecution1.getServiceExecutionSummaries().stream())
                      .map(ElementExecutionSummary::getInstancesCount)
                      .mapToInt(i -> i)
                      .sum();
            }
          }
        }
      }

      dayStats.add(new DayStat(totalCount, failureCount, instanceCount, timeOffset));
      aggTotalCount += totalCount;
      aggFailureCount += failureCount;
      aggInstanceCount += instanceCount;
    }

    return new AggregatedDayStats(aggTotalCount, aggFailureCount, aggInstanceCount, dayStats);
  }

  private void getTopServicesDeployed(List<TopConsumer> topConsumers, List<WorkflowExecution> wflExecutions) {
    Map<String, TopConsumer> topConsumerMap = new HashMap<>();
    if (isEmpty(wflExecutions)) {
      return;
    }
    for (WorkflowExecution execution : wflExecutions) {
      if (!ExecutionStatus.isFinalStatus(execution.getStatus())) {
        continue;
      }
      final List<ElementExecutionSummary> serviceExecutionSummaries = new ArrayList<>();
      if (execution.getWorkflowType() == PIPELINE && execution.getPipelineExecution() != null
          && execution.getPipelineExecution().getPipelineStageExecutions() != null) {
        execution.getPipelineExecution()
            .getPipelineStageExecutions()
            .stream()
            .filter(pipelineStageExecution -> pipelineStageExecution.getWorkflowExecutions() != null)
            .flatMap(pipelineStageExecution -> pipelineStageExecution.getWorkflowExecutions().stream())
            .filter(workflowExecution -> workflowExecution.getServiceExecutionSummaries() != null)
            .forEach(workflowExecution -> {
              serviceExecutionSummaries.addAll(workflowExecution.getServiceExecutionSummaries());
            });
      } else if (execution.getServiceExecutionSummaries() != null) {
        serviceExecutionSummaries.addAll(execution.getServiceExecutionSummaries());
      }
      Map<String, ElementExecutionSummary> serviceExecutionStatusMap = new HashMap<>();
      for (ElementExecutionSummary serviceExecutionSummary : serviceExecutionSummaries) {
        if (serviceExecutionSummary.getContextElement() == null) {
          continue;
        }
        String serviceId = serviceExecutionSummary.getContextElement().getUuid();
        serviceExecutionStatusMap.put(serviceId, serviceExecutionSummary);
      }
      for (ElementExecutionSummary serviceExecutionSummary : serviceExecutionStatusMap.values()) {
        String serviceId = serviceExecutionSummary.getContextElement().getUuid();
        ExecutionStatus serviceExecutionStatus = serviceExecutionSummary.getStatus();
        if (serviceExecutionStatus == null) {
          serviceExecutionStatus = execution.getStatus();
        }
        TopConsumer topConsumer;
        if (!topConsumerMap.containsKey(serviceId)) {
          TopConsumer tempConsumer = TopConsumer.builder()
                                         .appId(execution.getAppId())
                                         .appName(execution.getAppName())
                                         .serviceId(serviceId)
                                         .serviceName(serviceExecutionSummary.getContextElement().getName())
                                         .build();
          topConsumerMap.put(serviceId, tempConsumer);
          topConsumers.add(tempConsumer);
        }
        topConsumer = topConsumerMap.get(serviceId);
        if (serviceExecutionStatus.equals(SUCCESS)) {
          topConsumer.setSuccessfulActivityCount(topConsumer.getSuccessfulActivityCount() + 1);
          topConsumer.setTotalCount(topConsumer.getTotalCount() + 1);
        } else {
          topConsumer.setFailedActivityCount(topConsumer.getFailedActivityCount() + 1);
          topConsumer.setTotalCount(topConsumer.getTotalCount() + 1);
        }
      }
    }
  }

  private long getEpochMilliPSTZone(int days) {
    return EpochUtil.calculateEpochMilliOfStartOfDayForXDaysInPastFromNow(days, PST_ZONE_ID);
  }
}
