/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.ce;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.TimeUnit;
import javax.annotation.ParametersAreNonnullByDefault;

@TargetModule(HarnessModule._490_CE_COMMONS)
@OwnedBy(CE)
@Singleton
@ParametersAreNonnullByDefault
public class CeAccountExpirationCheckerImpl implements CeAccountExpirationChecker {
  @Inject private HPersistence persistence;
  private static final long CACHE_SIZE = 1000;

  private final LoadingCache<String, Boolean> accountIdToIsCeEnabled = Caffeine.newBuilder()
                                                                           .expireAfterWrite(1, TimeUnit.HOURS)
                                                                           .maximumSize(CACHE_SIZE)
                                                                           .build(this::isCeEnabledForAccount);

  @Override
  public void checkIsCeEnabled(String accountId) {
    if (!accountIdToIsCeEnabled.get(accountId)) {
      throw new InvalidRequestException("Cloud Cost Management is not enabled on account: " + accountId);
    }
  }

  private boolean isCeEnabledForAccount(String accountId) {
    Account account = persistence.createQuery(Account.class, excludeAuthority)
                          .filter(AccountKeys.cloudCostEnabled, Boolean.TRUE)
                          .field(AccountKeys.uuid)
                          .equal(accountId)
                          .get();
    return account != null;
  }
}
