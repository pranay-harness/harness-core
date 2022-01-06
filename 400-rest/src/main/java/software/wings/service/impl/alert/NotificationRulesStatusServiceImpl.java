/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.alert;

import software.wings.beans.AccountType;
import software.wings.beans.alert.NotificationRulesStatus;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.alert.NotificationRulesStatusService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

public class NotificationRulesStatusServiceImpl implements NotificationRulesStatusService {
  @Inject private WingsPersistence persistence;
  @Inject private AccountService accountService;

  @Override
  @Nonnull
  public NotificationRulesStatus get(String accountId) {
    Preconditions.checkArgument(StringUtils.isNotEmpty(accountId), "accountId can not be blank");
    NotificationRulesStatus status = persistence.get(NotificationRulesStatus.class, accountId);

    if (null == status) {
      String accountType = accountService.getAccountType(accountId).orElse(AccountType.PAID);
      // lazily update the status if it's not present in database
      if (accountType.equals(AccountType.PAID)) {
        return update(accountId, true);
      } else {
        return update(accountId, false);
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
