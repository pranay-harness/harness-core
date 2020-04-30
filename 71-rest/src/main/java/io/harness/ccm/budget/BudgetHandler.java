package io.harness.ccm.budget;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

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
import org.apache.http.client.utils.URIBuilder;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.impl.notifications.UserGroupBasedDispatcher;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.UserGroupService;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class BudgetHandler implements Handler<Budget> {
  private static final int THRESHOLD_CHECK_INTERVAL_MINUTE = 60;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject UserGroupService userGroupService;
  @Inject UserGroupBasedDispatcher userGroupBasedDispatcher;
  @Inject BudgetService budgetService;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Inject private UserServiceImpl userService;

  private static final String BUDGET_MAIL_ERROR = "Budget alert email couldn't be sent";
  private static final String BUDGET_DETAILS_URL_FORMAT = "/account/%s/continuous-efficiency/budget/%s";

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

    List<String> userGroupIds = Arrays.asList(Optional.ofNullable(budget.getUserGroupIds()).orElse(new String[0]));
    if (CollectionUtils.isEmpty(userGroupIds)) {
      logger.warn("The budget with id={} has no associated UserGroup.", budget.getUuid());
      return;
    }

    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    for (int i = 0; i < alertThresholds.length; i++) {
      if (alertThresholds[i].getAlertsSent() > 0) {
        continue;
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
          sendBudgetAlertMail(userGroup, budget.getUuid(), budget.getName(), alertThresholds[i], currentCost);
          budgetService.incAlertCount(budget, i);
          budgetService.setThresholdCrossedTimestamp(budget, i, Instant.now().toEpochMilli());
        }
      }
    }
  }

  private void sendBudgetAlertMail(
      UserGroup userGroup, String budgetId, String budgetName, AlertThreshold alertThreshold, double currentCost) {
    try {
      String accountId = userGroup.getAccountId();
      String budgetUrl = buildAbsoluteUrl(format(BUDGET_DETAILS_URL_FORMAT, accountId, budgetId), accountId);

      Map<String, String> templateModel = new HashMap<>();
      templateModel.put("url", budgetUrl);
      templateModel.put("BUDGET_NAME", budgetName);
      templateModel.put("THRESHOLD_PERCENTAGE", String.format("%.1f", alertThreshold.getPercentage()));
      templateModel.put("CURRENT_COST", String.format("%.2f", currentCost));

      userGroup.getMemberIds().forEach(memberId -> {
        User user = userService.get(memberId);
        templateModel.put("name", user.getName());
        EmailData emailData = EmailData.builder()
                                  .to(Arrays.asList(user.getEmail()))
                                  .templateName("ce_budget_alert")
                                  .templateModel(templateModel)
                                  .accountId(userGroup.getAccountId())
                                  .build();
        emailData.setCc(Collections.emptyList());
        emailData.setRetries(2);
        emailNotificationService.send(emailData);
      });
    } catch (URISyntaxException e) {
      logger.error(BUDGET_MAIL_ERROR, e);
    }
  }

  private String buildAbsoluteUrl(String fragment, String accountId) throws URISyntaxException {
    String baseUrl = subdomainUrlHelper.getPortalBaseUrl(accountId);
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  private boolean exceedsThreshold(double currentAmount, double thresholdAmount) {
    return currentAmount >= thresholdAmount;
  }

  private double getThresholdAmount(Budget budget, AlertThreshold alertThreshold) {
    switch (alertThreshold.getBasedOn()) {
      case ACTUAL_COST:
        return budget.getBudgetAmount() * alertThreshold.getPercentage() / 100;
      case FORECASTED_COST:
        return budgetService.getForecastCost(budget) * alertThreshold.getPercentage() / 100;
      default:
        return 0;
    }
  }
}
