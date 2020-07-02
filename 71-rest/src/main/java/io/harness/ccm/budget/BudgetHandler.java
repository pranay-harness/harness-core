package io.harness.ccm.budget;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.ccm.budget.entities.AlertThresholdBase.ACTUAL_COST;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static org.apache.commons.text.StrSubstitutor.replace;
import static software.wings.common.Constants.HARNESS_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.ccm.budget.entities.AlertThreshold;
import io.harness.ccm.budget.entities.Budget;
import io.harness.ccm.budget.entities.Budget.BudgetKeys;
import io.harness.ccm.communication.CESlackWebhookService;
import io.harness.ccm.communication.entities.CESlackWebhook;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import software.wings.beans.User;
import software.wings.beans.notification.SlackNotificationConfiguration;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.UserGroup;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.impl.notifications.UserGroupBasedDispatcher;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.service.intfc.UserGroupService;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
  @Inject private SlackNotificationService slackNotificationService;
  @Inject private CESlackWebhookService ceSlackWebhookService;
  @Inject private MorphiaPersistenceProvider<Budget> persistenceProvider;

  private static final String BUDGET_MAIL_ERROR = "Budget alert email couldn't be sent";
  private static final String BUDGET_DETAILS_URL_FORMAT = "/account/%s/continuous-efficiency/budget/%s";
  private static final String ACTUAL_COST_BUDGET = "cost";
  private static final String FORECASTED_COST_BUDGET = "forecasted cost";

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder()
            .name("BudgetProcessor")
            .poolSize(3)
            .interval(ofMinutes(THRESHOLD_CHECK_INTERVAL_MINUTE))
            .build(),
        BudgetHandler.class,
        MongoPersistenceIterator.<Budget, MorphiaFilterExpander<Budget>>builder()
            .clazz(Budget.class)
            .fieldName(BudgetKeys.alertIteration)
            .targetInterval(ofMinutes(THRESHOLD_CHECK_INTERVAL_MINUTE))
            .acceptableNoAlertDelay(ofSeconds(60))
            .handler(this)
            .filterExpander(q -> q.field(BudgetKeys.alertThresholds).exists())
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(Budget budget) {
    checkNotNull(budget.getAlertThresholds());
    checkNotNull(budget.getAccountId());

    List<String> emailAddresses =
        Lists.newArrayList(Optional.ofNullable(budget.getEmailAddresses()).orElse(new String[0]));
    List<String> userGroupIds = Arrays.asList(Optional.ofNullable(budget.getUserGroupIds()).orElse(new String[0]));
    emailAddresses.addAll(getEmailsForUserGroup(budget.getAccountId(), userGroupIds));
    CESlackWebhook slackWebhook = ceSlackWebhookService.getByAccountId(budget.getAccountId());
    if (slackWebhook != null && isEmpty(emailAddresses) && isEmpty(userGroupIds)) {
      logger.warn("The budget with id={} has no associated communication channels.", budget.getUuid());
      return;
    }

    AlertThreshold[] alertThresholds = budget.getAlertThresholds();
    for (int i = 0; i < alertThresholds.length; i++) {
      if (alertThresholds[i].getAlertsSent() > 0
          && budgetService.isAlertSentInCurrentMonth(alertThresholds[i].getCrossedAt())) {
        continue;
      }
      String costType = ACTUAL_COST_BUDGET;
      double currentCost;
      try {
        if (alertThresholds[i].getBasedOn() == ACTUAL_COST) {
          currentCost = budgetService.getActualCost(budget);
        } else {
          currentCost = budgetService.getForecastCost(budget);
          costType = FORECASTED_COST_BUDGET;
        }
        logger.info("{} has been spent under the budget with id={} ", currentCost, budget.getUuid());
      } catch (Exception e) {
        logger.error(e.getMessage());
        break;
      }

      if (exceedsThreshold(currentCost, getThresholdAmount(budget, alertThresholds[i]))) {
        sendBudgetAlertViaSlack(budget, alertThresholds[i], slackWebhook);
        sendBudgetAlertMail(budget.getAccountId(), emailAddresses, budget.getUuid(), budget.getName(),
            alertThresholds[i], currentCost, costType);
        budgetService.incAlertCount(budget, i);
        budgetService.setThresholdCrossedTimestamp(budget, i, Instant.now().toEpochMilli());
      }
    }
  }

  private void sendBudgetAlertViaSlack(Budget budget, AlertThreshold alertThreshold, CESlackWebhook slackWebhook) {
    if (slackWebhook == null) {
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
      UserGroup userGroup = userGroupService.get(accountId, userGroupId, true);
      emailAddresses.addAll(userGroup.getMemberIds()
                                .stream()
                                .map(memberId -> {
                                  User user = userService.get(memberId);
                                  return user.getEmail();
                                })
                                .collect(Collectors.toList()));
    }
    return emailAddresses;
  }

  private void sendBudgetAlertMail(String accountId, List<String> emailAddresses, String budgetId, String budgetName,
      AlertThreshold alertThreshold, double currentCost, String costType) {
    List<String> uniqueEmailAddresses = new ArrayList<>(new HashSet<>(emailAddresses));

    try {
      String budgetUrl = buildAbsoluteUrl(format(BUDGET_DETAILS_URL_FORMAT, accountId, budgetId), accountId);

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
