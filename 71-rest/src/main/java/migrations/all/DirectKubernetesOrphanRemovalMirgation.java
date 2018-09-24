package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.InfrastructureMapping;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by rsingh on 6/26/18.
 */
public class DirectKubernetesOrphanRemovalMirgation implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(DirectKubernetesOrphanRemovalMirgation.class);
  @Inject WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);

    Set<String> recordsToBeDeleted = new HashSet<>();
    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        logger.info("migrating for account {} id {}", account.getAccountName(), account.getUuid());
        int updated = 0;
        int deleted = 0;
        Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                      .filter("accountId", account.getUuid())
                                                      .filter("type", SettingVariableTypes.DIRECT);
        try (HIterator<EncryptedData> encryptedDataRecords = new HIterator<>(encryptedDataQuery.fetch())) {
          while (encryptedDataRecords.hasNext()) {
            EncryptedData encryptedData = encryptedDataRecords.next();
            Set<String> parentIds = encryptedData.getParentIds();
            if (!isEmpty(parentIds)) {
              boolean shouldUpdate = false;
              for (Iterator<String> parentIdIter = parentIds.iterator(); parentIdIter.hasNext();) {
                String parentId = parentIdIter.next();
                InfrastructureMapping infrastructureMapping =
                    wingsPersistence.get(InfrastructureMapping.class, parentId);
                if (infrastructureMapping == null) {
                  logger.info("removing {} from record {}", parentId, encryptedData.getUuid());
                  parentIdIter.remove();
                  shouldUpdate = true;
                }
              }
              if (isEmpty(parentIds)) {
                logger.info("should delete {}", encryptedData.getUuid());
                recordsToBeDeleted.add(encryptedData.getUuid());
                deleted++;
              }

              if (shouldUpdate) {
                wingsPersistence.save(encryptedData);
                updated++;
              }
            }
          }
        }
        logger.info("updated {} deleted {} for account {}", updated, deleted, account.getUuid());
        logger.info("orphan records: {}", recordsToBeDeleted);
      }
    }
  }
}
