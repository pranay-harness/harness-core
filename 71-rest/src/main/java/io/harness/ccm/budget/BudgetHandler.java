package io.harness.ccm.budget;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.BUDGET_NOTIFICATION;

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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

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
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(Budget budget) {
    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    if (null == alertThresholds) {
      return;
    }

    String accountId = budget.getAccountId();
    String userGroupId = budget.getUserGroupId();
    UserGroup userGroup = userGroupService.get(accountId, userGroupId, true);
    if (null == userGroupId || null == userGroup) {
      logger.warn("This budget has no associated UserGroup.");
      return;
    }
    for (int i = 0; i < alertThresholds.length; i++) {
      double currentCost = 0;
      try {
        currentCost = budgetService.getActualCost(budget);
      } catch (SQLException e) {
        logger.error(e.getMessage());
      }
      if (alertThresholds[i].getAlertsSent() <= 0
          && exceedsThreshold(currentCost, getThresholdAmount(budget, alertThresholds[i]))) {
        sendBudgetAlerts(accountId, userGroup);
        budgetService.incAlertCount(budget, i);
      }
    }
  }

  private boolean sendBudgetAlerts(String accountId, UserGroup userGroup) {
    Preconditions.checkNotNull(userGroup);
    Notification notification = InformationNotification.builder()
                                    .notificationTemplateId(BUDGET_NOTIFICATION.name())
                                    .notificationTemplateVariables(new HashMap<>())
                                    .accountId(accountId)
                                    .build();
    userGroupBasedDispatcher.dispatch(Arrays.asList(notification), userGroup);
    return true;
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
