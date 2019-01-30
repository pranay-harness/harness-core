package software.wings.service.intfc;

import software.wings.beans.alert.AlertNotificationRule;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;
import javax.annotation.Nonnull;

public interface AlertNotificationRuleService extends OwnedByAccount {
  AlertNotificationRule create(AlertNotificationRule rule);

  AlertNotificationRule update(AlertNotificationRule rule);

  AlertNotificationRule createDefaultRule(String accountId);

  @Nonnull List<AlertNotificationRule> getAll(String accountId);

  void deleteById(String ruleId, String accountId);
}
