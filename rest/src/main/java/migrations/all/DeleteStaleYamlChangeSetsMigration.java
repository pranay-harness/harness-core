package migrations.all;

import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.COMPLETED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.FAILED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.SKIPPED;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import java.util.List;

public class DeleteStaleYamlChangeSetsMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(DeleteStaleYamlChangeSetsMigration.class);
  public static final String BATCH_SIZE = "2000";
  public static final int RETENTION_PERIOD_IN_DAYS = 30;

  @Inject WingsPersistence wingsPersistence;
  @Inject YamlChangeSetService yamlChangeSetService;
  @Inject AccountService accountService;
  @Override
  public void migrate() {
    logger.info("Deleting stale YamlChangeSets");
    try {
      List<Account> accounts =
          accountService.list(wingsPersistence.query(Account.class, aPageRequest().addFieldsIncluded("_id").build()));
      for (Account account : accounts) {
        yamlChangeSetService.deleteChangeSets(account.getUuid(), new Status[] {COMPLETED, FAILED, SKIPPED},
            Integer.MAX_VALUE, BATCH_SIZE, RETENTION_PERIOD_IN_DAYS);
      }
    } catch (Exception e) {
      logger.error("Delete YamlChangeSet error", e);
    }
  }
}