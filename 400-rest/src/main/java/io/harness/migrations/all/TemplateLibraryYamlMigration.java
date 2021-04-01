package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.sync.YamlService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
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
