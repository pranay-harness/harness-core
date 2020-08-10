package io.harness.batch.processing.budgets.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.ccm.budget.entities.AlertThresholdBase.ACTUAL_COST;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static org.apache.commons.text.StrSubstitutor.replace;
import static software.wings.common.Constants.HARNESS_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.mail.CEMailNotificationService;
import io.harness.batch.processing.slackNotification.CESlackNotificationService;
import io.harness.ccm.budget.BudgetUtils;
import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.BudgetAlertsData;
import io.harness.ccm.communication.CESlackWebhookService;
import io.harness.ccm.communication.entities.CESlackWebhook;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.notification.SlackNotificationConfiguration;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.budget.BudgetTimescaleQueryHelper;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Singleton
@Slf4j
public class BudgetAlertsServiceImpl {
  @Autowired private TimeScaleDBService timeScaleDBService;
  @Autowired private CEMailNotificationService emailNotificationService;
  @Autowired private CESlackNotificationService slackNotificationService;
  @Autowired private BudgetTimescaleQueryHelper budgetTimescaleQueryHelper;
  @Autowired private BudgetUtils budgetUtils;
  @Autowired private CESlackWebhookService ceSlackWebhookService;
  @Autowired private BatchMainConfig mainConfiguration;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;

  private static final String BUDGET_MAIL_ERROR = "Budget alert email couldn't be sent";
  private static final String BUDGET_DETAILS_URL_FORMAT = "/account/%s/continuous-efficiency/budget/%s";
  private static final String ACTUAL_COST_BUDGET = "cost";
  private static final String FORECASTED_COST_BUDGET = "forecasted cost";

  public void sendBudgetAlerts() {
    List<Account> ceEnabledAccounts = cloudToHarnessMappingService.getCeEnabledAccounts();
    List<String> accountIds = ceEnabledAccounts.stream().map(Account::getUuid).collect(Collectors.toList());
    accountIds.forEach(accountId -> {
      List<Budget> budgets = budgetUtils.listBudgetsForAccount(accountId);
      budgets.forEach(budget -> {
        try {
          checkAndSendAlerts(budget);
        } catch (Exception e) {
          logger.error("Can't send alert for budget : {}, Exception: ", budget.getUuid(), e);
        }
      });
    });
  }

  private void checkAndSendAlerts(Budget budget) {
    checkNotNull(budget.getAlertThresholds());
    checkNotNull(budget.getAccountId());

    List<String> emailAddresses =
        Lists.newArrayList(Optional.ofNullable(budget.getEmailAddresses()).orElse(new String[0]));
    List<String> userGroupIds = Arrays.asList(Optional.ofNullable(budget.getUserGroupIds()).orElse(new String[0]));
    emailAddresses.addAll(getEmailsForUserGroup(budget.getAccountId(), userGroupIds));
    CESlackWebhook slackWebhook = ceSlackWebhookService.getByAccountId(budget.getAccountId());
    if (slackWebhook == null && isEmpty(emailAddresses) && isEmpty(userGroupIds)) {
      logger.warn("The budget with id={} has no associated communication channels.", budget.getUuid());
      return;
    }

    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    for (int i = 0; i < alertThresholds.length; i++) {
      double actualCost = budgetUtils.getActualCost(budget);
      BudgetAlertsData data = BudgetAlertsData.builder()
                                  .accountId(budget.getAccountId())
                                  .actualCost(actualCost)
                                  .budgetedCost(budget.getBudgetAmount())
                                  .budgetId(budget.getUuid())
                                  .alertThreshold(alertThresholds[i].getPercentage())
                                  .time(System.currentTimeMillis())
                                  .build();

      if (budgetUtils.isAlertSentInCurrentMonth(
              budgetTimescaleQueryHelper.getLastAlertTimestamp(data, budget.getAccountId()))) {
        continue;
      }
      String costType = ACTUAL_COST_BUDGET;
      double currentCost;
      try {
        if (alertThresholds[i].getBasedOn() == ACTUAL_COST) {
          currentCost = actualCost;
        } else {
          currentCost = budgetUtils.getForecastCost(budget);
          costType = FORECASTED_COST_BUDGET;
        }
        logger.info("{} has been spent under the budget with id={} ", currentCost, budget.getUuid());
      } catch (Exception e) {
        logger.error(e.getMessage());
        break;
      }

      if (exceedsThreshold(currentCost, getThresholdAmount(budget, alertThresholds[i]))) {
        try {
          sendBudgetAlertViaSlack(budget, alertThresholds[i], slackWebhook);
        } catch (Exception e) {
          logger.error("Notification via slack not send : ", e);
        }
        sendBudgetAlertMail(budget.getAccountId(), emailAddresses, budget.getUuid(), budget.getName(),
            alertThresholds[i], currentCost, costType);
        // insert in timescale table
        budgetTimescaleQueryHelper.insertAlertEntryInTable(data, budget.getAccountId());
      }
    }
  }

  private void sendBudgetAlertViaSlack(Budget budget, AlertThreshold alertThreshold, CESlackWebhook slackWebhook) {
    if (slackWebhook == null || !budget.isNotifyOnSlack()) {
      return;
    }
    SlackNotificationConfiguration slackConfig =
        new SlackNotificationSetting("#ccm-test", slackWebhook.getWebhookUrl());
    String slackMessageTemplate =
        "The cost associated with *${BUDGET_NAME}* has reached a limit of ${THRESHOLD_PERCENTAGE}%.";
    Map<String, String> params = ImmutableMap.<String, String>builder()
                                     .put("THRESHOLD_PERCENTAGE", String.format("%.1f", alertThreshold.getPercentage()))
                                     .put("BUDGET_NAME", budget.getName())
                                     .build();
    String slackMessage = replace(slackMessageTemplate, params);
    slackNotificationService.sendMessage(
        slackConfig, stripToEmpty(slackConfig.getName()), HARNESS_NAME, slackMessage, budget.getAccountId());
  }

  private List<String> getEmailsForUserGroup(String accountId, List<String> userGroupIds) {
    List<String> emailAddresses = new ArrayList<>();
    for (String userGroupId : userGroupIds) {
      UserGroup userGroup = cloudToHarnessMappingService.getUserGroup(accountId, userGroupId, true);
      if (userGroup != null) {
        emailAddresses.addAll(userGroup.getMemberIds()
                                  .stream()
                                  .map(memberId -> {
                                    User user = cloudToHarnessMappingService.getUser(memberId);
                                    return user.getEmail();
                                  })
                                  .collect(Collectors.toList()));
      }
    }
    return emailAddresses;
  }

  private void sendBudgetAlertMail(String accountId, List<String> emailAddresses, String budgetId, String budgetName,
      AlertThreshold alertThreshold, double currentCost, String costType) {
    List<String> uniqueEmailAddresses = new ArrayList<>(new HashSet<>(emailAddresses));

    try {
      String budgetUrl = buildAbsoluteUrl(format(BUDGET_DETAILS_URL_FORMAT, accountId, budgetId));

      Map<String, String> templateModel = new HashMap<>();
      templateModel.put("url", budgetUrl);
      templateModel.put("BUDGET_NAME", budgetName);
      templateModel.put("THRESHOLD_PERCENTAGE", String.format("%.1f", alertThreshold.getPercentage()));
      templateModel.put("CURRENT_COST", String.format("%.2f", currentCost));
      templateModel.put("COST_TYPE", costType);

      uniqueEmailAddresses.forEach(emailAddress -> {
        templateModel.put("name", emailAddress.substring(0, emailAddress.lastIndexOf('@')));
        EmailData emailData = EmailData.builder()
                                  .to(singletonList(emailAddress))
                                  .templateName("ce_budget_alert")
                                  .templateModel(ImmutableMap.copyOf(templateModel))
                                  .accountId(accountId)
                                  .build();
        emailData.setCc(emptyList());
        emailData.setRetries(0);
        emailNotificationService.send(emailData);
      });
    } catch (URISyntaxException e) {
      logger.error(BUDGET_MAIL_ERROR, e);
    }
  }

  private String buildAbsoluteUrl(String fragment) throws URISyntaxException {
    String baseUrl = mainConfiguration.getBaseUrl();
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
        return budgetUtils.getForecastCost(budget) * alertThreshold.getPercentage() / 100;
      default:
        return 0;
    }
  }
}
