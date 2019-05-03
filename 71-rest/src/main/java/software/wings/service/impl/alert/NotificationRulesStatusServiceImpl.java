package software.wings.service.impl.alert;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import software.wings.beans.alert.NotificationRulesStatus;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.alert.NotificationRulesStatusService;

import javax.annotation.Nonnull;

public class NotificationRulesStatusServiceImpl implements NotificationRulesStatusService {
  @Inject private WingsPersistence persistence;
  @Inject private AccountService accountService;

  @Override
  @Nonnull
  public NotificationRulesStatus get(String accountId) {
    Preconditions.checkArgument(StringUtils.isNotEmpty(accountId), "accountId can not be blank");
    NotificationRulesStatus status = persistence.get(NotificationRulesStatus.class, accountId);

    if (null == status) {
      // lazily update the status if it's not present in database
      if (accountService.isCommunityAccount(accountId) || accountService.isTrialAccount(accountId)) {
        return update(accountId, false);
      } else {
        return update(accountId, true);
      }
    } else {
      return status;
    }
  }

  @Override
  public NotificationRulesStatus update(String accountId, boolean enabled) {
    Preconditions.checkArgument(StringUtils.isNotEmpty(accountId), "accountId can not be blank");

    NotificationRulesStatus status = new NotificationRulesStatus(accountId, enabled);
    persistence.save(status);
    return status;
  }
}
