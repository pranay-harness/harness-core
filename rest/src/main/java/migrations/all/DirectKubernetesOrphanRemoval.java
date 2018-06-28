package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.dl.HIterator;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 6/26/18.
 */
public class DirectKubernetesOrphanRemoval implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(DirectKubernetesOrphanRemoval.class);
  @Inject WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        Set<String> recordsToBeDeleted = new HashSet<>();
        int deleted = 0;
        logger.info("migrating for account {} id {}", account.getAccountName(), account.getUuid());
        Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                      .filter("accountId", account.getUuid())
                                                      .filter("type", SettingVariableTypes.DIRECT);
        try (HIterator<EncryptedData> encryptedDataRecords = new HIterator<>(encryptedDataQuery.fetch())) {
          while (encryptedDataRecords.hasNext()) {
            EncryptedData encryptedData = encryptedDataRecords.next();
            Set<String> parentIds = encryptedData.getParentIds();
            if (isEmpty(parentIds)) {
              recordsToBeDeleted.add(encryptedData.getUuid());
              deleted++;
            }
          }
          recordsToBeDeleted.forEach(recordId -> {
            logger.info("deleting encrypted record {}", recordId);
            wingsPersistence.delete(EncryptedData.class, recordId);
          });
        }
        logger.info("deleted {} records for account {}", deleted, account.getUuid());
      }
    }
  }
}
