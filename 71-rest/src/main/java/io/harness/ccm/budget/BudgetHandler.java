package io.harness.ccm.budget;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.BUDGET_NOTIFICATION;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.hazelcast.util.Preconditions;
import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.Budget.BudgetKeys;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.security.UserGroup;
import software.wings.service.impl.notifications.UserGroupBasedDispatcher;
import software.wings.service.intfc.UserGroupService;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class BudgetHandler implements Handler<Budget> {
  private static final int THRESHOLD_CHECK_INTERVAL_MINUTE = 60;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject UserGroupService userGroupService;
  @Inject UserGroupBasedDispatcher userGroupBasedDispatcher;
  @Inject BudgetService budgetService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder()
            .name("BudgetProcessor")
            .poolSize(3)
            .interval(ofMinutes(THRESHOLD_CHECK_INTERVAL_MINUTE))
            .build(),
        BudgetHandler.class,
        MongoPersistenceIterator.<Budget>builder()
            .clazz(Budget.class)
            .fieldName(BudgetKeys.alertIteration)
            .targetInterval(ofMinutes(THRESHOLD_CHECK_INTERVAL_MINUTE))
            .acceptableNoAlertDelay(ofSeconds(60))
            .handler(this)
            .filterExpander(q -> q.field(BudgetKeys.alertThresholds).exists())
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(Budget budget) {
    Preconditions.checkNotNull(budget.getAlertThresholds());
    Preconditions.checkNotNull(budget.getAccountId());

    List<String> userGroupIds = Arrays.asList(budget.getUserGroupIds());
    if (CollectionUtils.isEmpty(userGroupIds)) {
      logger.warn("The budget with id={} has no associated UserGroup.", budget.getUuid());
      return;
    }

    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    for (int i = 0; i < alertThresholds.length; i++) {
      if (alertThresholds[i].getAlertsSent() > 0) {
        break;
      }

      double currentCost;
      try {
        currentCost = budgetService.getActualCost(budget);
        logger.info("{} has been spent under the budget with id={} ", currentCost, budget.getUuid());
      } catch (Exception e) {
        logger.error(e.getMessage());
        break;
      }

      if (exceedsThreshold(currentCost, getThresholdAmount(budget, alertThresholds[i]))) {
        for (String userGroupId : userGroupIds) {
          UserGroup userGroup = userGroupService.get(budget.getAccountId(), userGroupId, true);
          Notification budgetNotification =
              getBudgetNotification(budget.getAccountId(), budget.getName(), alertThresholds[i], currentCost);
          userGroupBasedDispatcher.dispatch(Arrays.asList(budgetNotification), userGroup);
          budgetService.incAlertCount(budget, i);
          budgetService.setThresholdCrossedTimestamp(budget, i, Instant.now().toEpochMilli());
        }
      }
    }
  }

  private Notification getBudgetNotification(
      String accountId, String budgetName, AlertThreshold alertThreshold, double currentCost) {
    return InformationNotification.builder()
        .notificationTemplateId(BUDGET_NOTIFICATION.name())
        .notificationTemplateVariables(
            ImmutableMap.<String, String>builder()
                .put("BUDGET_NAME", budgetName)
                .put("THRESHOLD_PERCENTAGE", String.format("%.1f", alertThreshold.getPercentage()))
                .put("CURRENT_COST", String.format("%.2f", currentCost))
                .build())
        .accountId(accountId)
        .build();
  }

  private boolean exceedsThreshold(double currentAmount, double thresholdAmount) {
    return currentAmount >= thresholdAmount;
  }

  private double getThresholdAmount(Budget budget, AlertThreshold alertThreshold) {
    switch (alertThreshold.getBasedOn()) {
      case ACTUAL_COST:
        return budget.getBudgetAmount() * alertThreshold.getPercentage();
      case FORECASTED_COST:
        return budgetService.getForecastCost(budget) * alertThreshold.getPercentage();
      default:
        return 0;
    }
  }
}
