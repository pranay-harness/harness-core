/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security;

import io.harness.security.KeySource;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

// TODO: (CCM-95) Move this out to the shared auth dependency.
@ParametersAreNonnullByDefault
@Singleton
public class AccountKeySource implements KeySource {
  private final LoadingCache<String, String> keyCache;

  @Inject
  public AccountKeySource(WingsPersistence wingsPersistence) {
    this.keyCache = Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterWrite(1, TimeUnit.MINUTES)
                        .build(accountId
                            -> Optional.ofNullable(wingsPersistence.get(Account.class, accountId))
                                   .map(Account::getAccountKey)
                                   .orElse(null));
  }

  @Nullable
  @Override
  public String fetchKey(String accountId) {
    return keyCache.get(accountId);
  }
}
