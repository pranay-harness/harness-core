package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceVariableService;

/**
 * Created by rsingh on 6/1/18.
 */
@Slf4j
public class SecretTextFilterMigration implements Migration {
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
