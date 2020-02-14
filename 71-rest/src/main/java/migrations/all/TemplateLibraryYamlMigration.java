package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.sync.YamlService;

@Slf4j
public class TemplateLibraryYamlMigration implements Migration {
  private static final String DEBUG_LINE = "TEMPLATE_YAML_SUPPORT: ";
  @Inject YamlService yamlService;
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info(String.join(DEBUG_LINE, " Starting Migration For Template Library Yaml"));
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        logger.info(String.join(
            DEBUG_LINE, " Starting Migration For Template Library Yaml for account", account.getAccountName()));
        yamlService.syncYamlTemplate(account.getUuid());
      }
    }

    logger.info(String.join(DEBUG_LINE, " Completed triggering migration for Template Library Yaml"));
  }
}
