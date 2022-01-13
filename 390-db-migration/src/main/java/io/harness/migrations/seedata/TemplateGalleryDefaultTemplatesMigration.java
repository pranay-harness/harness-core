/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.seedata;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;

import static java.lang.String.format;

import io.harness.migrations.SeedDataMigration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by anubhaw on 8/20/18.
 */
@Slf4j
public class TemplateGalleryDefaultTemplatesMigration implements SeedDataMigration {
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      boolean rootTemplateGalleryDoesNotExist = wingsPersistence.createQuery(TemplateGallery.class)
                                                    .field(TemplateGallery.NAME_KEY)
                                                    .equal(HARNESS_GALLERY)
                                                    .field("accountId")
                                                    .equal(GLOBAL_ACCOUNT_ID)
                                                    .getKey()
          == null;
      if (rootTemplateGalleryDoesNotExist) {
        log.error("TemplateGalleryDefaultTemplatesMigration root template gallery not found");
        templateGalleryService.loadHarnessGallery(); // takes care of copying templates to individual accounts
        templateGalleryService.copyHarnessTemplates();
      } else {
        // in case previous migration failed while copying Harness templates
        copyHarnessTemplateToAccounts();
      }
    } catch (Exception ex) {
      log.error("TemplateGalleryDefaultTemplatesMigration failed", ex);
    }
  }

  private void copyHarnessTemplateToAccounts() {
    try (HIterator<Account> records = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      for (Account account : records) {
        try {
          log.info("TemplateGalleryDefaultTemplatesMigration started for account [{}]", account.getUuid());
          boolean templateGalleryDoesNotExist = wingsPersistence.createQuery(TemplateGallery.class)
                                                    .field(TemplateGallery.NAME_KEY)
                                                    .equal(account.getAccountName())
                                                    .field("accountId")
                                                    .equal(account.getUuid())
                                                    .getKey()
              == null;
          if (templateGalleryDoesNotExist) {
            if (!GLOBAL_ACCOUNT_ID.equals(account.getUuid())) {
              templateGalleryService.copyHarnessTemplatesToAccount(account.getUuid(), account.getAccountName());
            }
          } else {
            log.info("TemplateGalleryDefaultTemplatesMigration gallery already exists for account [{}]. do nothing",
                account.getUuid());
          }
          log.info("TemplateGalleryDefaultTemplatesMigration finished for account [{}]", account.getUuid());
        } catch (Exception ex) {
          log.error(format("TemplateGalleryDefaultTemplatesMigration failed for account [%s]", account.getUuid()), ex);
        }
      }
    }
  }
}
