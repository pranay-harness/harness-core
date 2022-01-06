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
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.sync.YamlService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TemplateLibraryYamlMigration implements Migration {
  private static final String DEBUG_LINE = "TEMPLATE_YAML_SUPPORT: ";
  @Inject YamlService yamlService;
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info(String.join(DEBUG_LINE, " Starting Migration For Template Library Yaml"));
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        log.info(String.join(
            DEBUG_LINE, " Starting Migration For Template Library Yaml for account", account.getAccountName()));
        yamlService.syncYamlTemplate(account.getUuid());
      }
    }

    log.info(String.join(DEBUG_LINE, " Completed triggering migration for Template Library Yaml"));
  }
}
