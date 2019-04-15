package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Account;
import software.wings.beans.alert.Alert;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DeleteCVAlertsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<Account> harnessAccounts =
        wingsPersistence.createQuery(Account.class).filter("accountName", "Harness.io").asList();
    if (isEmpty(harnessAccounts)) {
      logger.info("There are no harness accounts in DeleteCVAlertsMigration. Returning");
      return;
    }

    List<String> harnessAccountIds = new ArrayList<>();
    harnessAccounts.forEach(account -> harnessAccountIds.add(account.getUuid()));

    Query<Alert> alertQuery = wingsPersistence.createQuery(Alert.class)
                                  .field("accountId")
                                  .notIn(harnessAccountIds)
                                  .filter("type", "CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT");

    List<Alert> alerts = alertQuery.asList();
    logger.info("Total number of CV Datacollection alerts to be deleted {}", alerts.size());
    wingsPersistence.delete(alertQuery);
    logger.info("{} CV Datacollection alerts have been deleted", alerts.size());
  }
}
