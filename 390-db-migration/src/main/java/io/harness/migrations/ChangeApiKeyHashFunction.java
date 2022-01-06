/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.beans.PageResponse;
import io.harness.hash.HashUtils;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.ApiKeyEntry.ApiKeyEntryKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ApiKeyService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeApiKeyHashFunction implements Migration {
  @Inject WingsPersistence wingsPersistence;
  @Inject ApiKeyService apiKeyService;

  @Override
  public void migrate() {
    log.info("Starting migration for new API key hash function.");
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        try {
          final PageResponse<ApiKeyEntry> apiKeys =
              apiKeyService.list(aPageRequest().build(), account.getUuid(), false, true);
          apiKeys.getResponse().forEach(apiKey -> {
            final String decryptedKey = apiKey.getDecryptedKey();
            final String shaHash = HashUtils.calculateSha256(decryptedKey);
            wingsPersistence.updateField(ApiKeyEntry.class, apiKey.getUuid(), ApiKeyEntryKeys.sha256Hash, shaHash);
            apiKeyService.evictAndRebuildPermissions(account.getUuid(), true);
          });
          log.info("Migration of api Key completed for account {}", account.getUuid());
        } catch (Exception ex) {
          log.error("Error while updating api key in account {}", account.getUuid(), ex);
        }
      }
    }
    log.info("Completed migration to new API Key hash function.");
  }
}
