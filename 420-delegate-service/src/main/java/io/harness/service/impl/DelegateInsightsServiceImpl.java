package io.harness.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateInsightsService;

import software.wings.beans.DelegateTaskUsageInsights;
import software.wings.beans.DelegateTaskUsageInsights.DelegateTaskUsageInsightsKeys;
import software.wings.beans.DelegateTaskUsageInsightsEventType;
import software.wings.service.impl.DelegateTaskStatusObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
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
}
