package migrations.all;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.dl.HIterator;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceVariableService;

/**
 * Created by rsingh on 6/1/18.
 */
public class SecretTextFilterMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceVariableService serviceVariableService;

  @Override
  public void migrate() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        serviceVariableService.updateSearchTagsForSecrets(account.getUuid());
      }
    }
  }
}
