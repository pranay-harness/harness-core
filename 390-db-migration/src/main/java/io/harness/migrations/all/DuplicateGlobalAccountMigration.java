/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import io.harness.migrations.Migration;

import software.wings.service.intfc.template.TemplateGalleryService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DuplicateGlobalAccountMigration implements Migration {
  @Inject private TemplateGalleryService templateGalleryService;
  @Override
  public void migrate() {
    log.info("Deleting template gallery for Account Name: Global");
    templateGalleryService.deleteAccountGalleryByName(GLOBAL_ACCOUNT_ID, "Global");
    log.info("Finished deleting template gallery for Account Name: Global");
  }
}
