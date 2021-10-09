package io.harness.migrations.all;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceVariableService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

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
      for (Account account : records) {
        int updatedRecords = serviceVariableService.updateSearchTagsForSecrets(account.getUuid());
        log.info("updated {} for account {}", updatedRecords, account.getUuid());
      }
    }
  }
}
