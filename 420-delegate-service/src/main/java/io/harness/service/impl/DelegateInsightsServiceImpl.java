package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateInsightsService;

import software.wings.beans.DelegateInsightsBarDetails;
import software.wings.beans.DelegateInsightsDetails;
import software.wings.beans.DelegateInsightsSummary;
import software.wings.beans.DelegateInsightsSummary.DelegateInsightsSummaryKeys;
import software.wings.beans.DelegateTaskUsageInsights;
import software.wings.beans.DelegateTaskUsageInsights.DelegateTaskUsageInsightsKeys;
import software.wings.beans.DelegateTaskUsageInsightsEventType;
import software.wings.service.impl.DelegateTaskStatusObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
public class DelegateInsightsServiceImpl implements DelegateInsightsService, DelegateTaskStatusObserver {
  @Inject private HPersistence persistence;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public void onTaskAssigned(String accountId, String taskId, String delegateId, String delegateGroupId) {
    if (featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, accountId)) {
      String finalDelegateGroupId = isEmpty(delegateGroupId) ? delegateId : delegateGroupId;

      DelegateTaskUsageInsights delegateTaskUsageInsightsCreateEvent = createDelegateTaskUsageInsightsEvent(
          accountId, taskId, delegateId, finalDelegateGroupId, DelegateTaskUsageInsightsEventType.STARTED);

      DelegateTaskUsageInsights delegateTaskUsageInsightsUnknownEvent = createDelegateTaskUsageInsightsEvent(
          accountId, taskId, delegateId, finalDelegateGroupId, DelegateTaskUsageInsightsEventType.UNKNOWN);

      persistence.save(delegateTaskUsageInsightsCreateEvent);
      persistence.save(delegateTaskUsageInsightsUnknownEvent);
    }
  }

  @Override
  public void onTaskCompleted(
      String accountId, String taskId, String delegateId, DelegateTaskUsageInsightsEventType eventType) {
    if (featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, accountId)) {
      Query<DelegateTaskUsageInsights> filterQuery =
          persistence.createQuery(DelegateTaskUsageInsights.class)
              .filter(DelegateTaskUsageInsightsKeys.accountId, accountId)
              .filter(DelegateTaskUsageInsightsKeys.taskId, taskId)
              .filter(DelegateTaskUsageInsightsKeys.delegateId, delegateId)
              .filter(DelegateTaskUsageInsightsKeys.eventType, DelegateTaskUsageInsightsEventType.UNKNOWN);

      UpdateOperations<DelegateTaskUsageInsights> updateOperations =
          persistence.createUpdateOperations(DelegateTaskUsageInsights.class)
              .set(DelegateTaskUsageInsightsKeys.eventType, eventType)
              .set(DelegateTaskUsageInsightsKeys.timestamp, System.currentTimeMillis());

      persistence.findAndModify(filterQuery, updateOperations, HPersistence.returnNewOptions);
    }
  }

  private DelegateTaskUsageInsights createDelegateTaskUsageInsightsEvent(String accountId, String taskId,
      String delegateId, String delegateGroupId, DelegateTaskUsageInsightsEventType eventType) {
    return DelegateTaskUsageInsights.builder()
        .accountId(accountId)
        .taskId(taskId)
        .eventType(eventType)
        .timestamp(System.currentTimeMillis())
        .delegateId(delegateId)
        .delegateGroupId(delegateGroupId)
        .build();
  }

  @Override
  public DelegateInsightsDetails retrieveDelegateInsightsDetails(
      String accountId, String delegateGroupId, long startTimestamp) {
    List<DelegateInsightsBarDetails> insightsBarDetails = new ArrayList<>();

    if (featureFlagService.isEnabled(FeatureName.DELEGATE_INSIGHTS_ENABLED, accountId)) {
      Map<Long, List<DelegateInsightsSummary>> delegateGroupInsights =
          persistence.createQuery(DelegateInsightsSummary.class)
              .filter(DelegateInsightsSummaryKeys.accountId, accountId)
              .filter(DelegateInsightsSummaryKeys.delegateGroupId, delegateGroupId)
              .field(DelegateInsightsSummaryKeys.periodStartTime)
              .greaterThan(startTimestamp)
              .asList()
              .stream()
              .collect(Collectors.groupingBy(DelegateInsightsSummary::getPeriodStartTime));

      for (Map.Entry<Long, List<DelegateInsightsSummary>> mapEntry : delegateGroupInsights.entrySet()) {
        DelegateInsightsBarDetails barInsights =
            DelegateInsightsBarDetails.builder().timeStamp(mapEntry.getKey()).build();
        mapEntry.getValue().forEach(
            event -> barInsights.getCounts().add(Pair.of(event.getInsightsType(), event.getCount())));

        insightsBarDetails.add(barInsights);
      }
    }

    return DelegateInsightsDetails.builder().insights(insightsBarDetails).build();
  }
}
