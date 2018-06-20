package migrations.all;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.dl.HIterator;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceVariableService;

/**
 * Created by rsingh on 6/1/18.
 */
public class SecretTextFilterMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(SecretTextFilterMigration.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceVariableService serviceVariableService;

  @Override
  public void migrate() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        int updatedRecords = serviceVariableService.updateSearchTagsForSecrets(account.getUuid());
        logger.info("updated {} for account {}", updatedRecords, account.getUuid());
      }
    }
  }
}
