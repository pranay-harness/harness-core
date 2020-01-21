package io.harness.ccm.budget;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
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
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.security.UserGroup;
import software.wings.service.impl.notifications.UserGroupBasedDispatcher;
import software.wings.service.intfc.UserGroupService;

import java.util.Arrays;

@Slf4j
public class BudgetHandler implements Handler<Budget> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject UserGroupService userGroupService;
  @Inject UserGroupBasedDispatcher userGroupBasedDispatcher;
  @Inject BudgetService budgetService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("BudgetProcessor").poolSize(3).interval(ofSeconds(3)).build(),
        BudgetHandler.class,
        MongoPersistenceIterator.<Budget>builder()
            .clazz(Budget.class)
            .fieldName(BudgetKeys.alertIteration)
            .targetInterval(ofSeconds(60))
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

    String userGroupId = budget.getUserGroupId();
    UserGroup userGroup = userGroupService.get(budget.getAccountId(), userGroupId, true);
    if (null == userGroupId || null == userGroup) {
      logger.warn("The budget with id={} has no associated UserGroup.", budget.getUuid());
      return;
    }

    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    for (int i = 0; i < alertThresholds.length; i++) {
      if (alertThresholds[i].getAlertsSent() > 0) {
        break;
      }

      double currentCost = 0;
      try {
        currentCost = budgetService.getActualCost(budget);
        logger.info("{} has been spent under the budget with id={} ", currentCost, budget.getUuid());
      } catch (Exception e) {
        logger.error(e.getMessage());
        break;
      }

      if (exceedsThreshold(currentCost, getThresholdAmount(budget, alertThresholds[i]))) {
        Notification budgetNotification =
            getBudgetNotification(budget.getAccountId(), budget.getName(), alertThresholds[i], currentCost);
        userGroupBasedDispatcher.dispatch(Arrays.asList(budgetNotification), userGroup);
        budgetService.incAlertCount(budget, i);
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
