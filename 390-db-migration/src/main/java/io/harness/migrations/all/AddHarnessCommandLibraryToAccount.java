/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddHarnessCommandLibraryToAccount implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject TemplateGalleryService templateGalleryService;
  private static final String DEBUG_LINE = "HARNESS_GALLERY_ADDITION: ";

  @Override
  public void migrate() {
    log.info(DEBUG_LINE + "Starting migration for adding new  template gallery");

    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createAuthorizedQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        if (wingsPersistence.createQuery(TemplateGallery.class)
                .filter(TemplateGallery.GALLERY_KEY, TemplateGallery.GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY)
                .filter(TemplateGallery.ACCOUNT_ID_KEY2, account.getUuid())
                .get()
            == null) {
          try {
            templateGalleryService.saveHarnessCommandLibraryGalleryToAccount(
                account.getUuid(), account.getAccountName());
          } catch (Exception e) {
            log.error(DEBUG_LINE + "Cannot add harness gallery to account" + account.getUuid(), e);
          }
        }
        log.info(DEBUG_LINE + "Saved Harness Command library gallery to account {}", account.getUuid());
      }
    }
  }
}
