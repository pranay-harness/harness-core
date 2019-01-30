package software.wings.service.impl;

import static io.harness.persistence.UuidAccess.ID_KEY;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.alerts.AlertCategory;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertNotificationRuleService;

import java.util.List;
import java.util.Optional;

@Singleton
public class AlertNotificationRuleServiceImpl implements AlertNotificationRuleService {
  private static final Logger logger = LoggerFactory.getLogger(AlertNotificationRuleServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public AlertNotificationRule create(AlertNotificationRule rule) {
    validateCreate(rule);
    return wingsPersistence.saveAndGet(AlertNotificationRule.class, rule);
  }

  private void validateCreate(AlertNotificationRule rule) {
    if (rule.isDefault() && isDefaultAlertNotificationRuleCreated(rule.getAccountId())) {
      throw new InvalidRequestException("Default alert notification rule already exists");
    }
  }

  private boolean isDefaultAlertNotificationRuleCreated(String accountId) {
    return getDefaultAlertNotificationRule(accountId).isPresent();
  }

  private Optional<AlertNotificationRule> getDefaultAlertNotificationRule(String accountId) {
    List<AlertNotificationRule> rules = wingsPersistence.createQuery(AlertNotificationRule.class)
                                            .filter(ACCOUNT_ID_KEY, accountId)
                                            .filter(AlertNotificationRule.ALERT_CATEGORY, AlertCategory.All)
                                            .asList();
    return rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0));
  }

  @Override
  public AlertNotificationRule update(AlertNotificationRule rule) {
    validateUpdate(rule);
    return wingsPersistence.saveAndGet(AlertNotificationRule.class, rule);
  }

  private void validateUpdate(AlertNotificationRule rule) {
    Optional<AlertNotificationRule> existingRule = getById(rule.getAccountId(), rule.getUuid());
    if (!existingRule.isPresent()) {
      throw new InvalidRequestException(
          "Can not update alert notification rule. No such alert notification rule exists");
    }
    if (!existingRule.get().isDefault() && rule.isDefault()
        && isDefaultAlertNotificationRuleCreated(rule.getAccountId())) {
      throw new InvalidRequestException(
          "Can not update alert notification rule. Default alert notification rule already exists");
    }
  }

  private Optional<AlertNotificationRule> getById(String accountId, String ruleId) {
    List<AlertNotificationRule> rules = wingsPersistence.createQuery(AlertNotificationRule.class)
                                            .filter(ACCOUNT_ID_KEY, accountId)
                                            .filter(ID_KEY, ruleId)
                                            .asList();
    return rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0));
  }

  @Override
  public List<AlertNotificationRule> getAll(String accountId) {
    Query<AlertNotificationRule> query =
        wingsPersistence.createQuery(AlertNotificationRule.class).filter(ACCOUNT_ID_KEY, accountId);

    return query.asList();
  }

  @Override
  public void deleteById(String ruleId, String accountId) {
    Optional<AlertNotificationRule> defaultAlertNotificationRule = getDefaultAlertNotificationRule(accountId);
    if (defaultAlertNotificationRule.isPresent() && defaultAlertNotificationRule.get().getUuid().equals(ruleId)) {
      throw new InvalidRequestException("Default alert notification rule can not be deleted");
    }
    wingsPersistence.delete(wingsPersistence.createQuery(AlertNotificationRule.class)
                                .filter(ACCOUNT_ID_KEY, accountId)
                                .filter(ID_KEY, ruleId));
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(AlertNotificationRule.class).filter(ACCOUNT_ID_KEY, accountId));
  }
}
