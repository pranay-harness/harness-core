package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.command.Command;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mongodb.morphia.Key;

public class AddAccountIdToCommandCollection extends AddAccountIdToAppEntities {
  @Override
  public void migrate() {
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class).project(Account.ID_KEY2, true).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();

        List<Key<Application>> appIdKeyList = wingsPersistence.createQuery(Application.class)
                                                  .filter(ApplicationKeys.accountId, account.getUuid())
                                                  .asKeyList();
        if (isNotEmpty(appIdKeyList)) {
          Set<String> appIdSet =
              appIdKeyList.stream().map(applicationKey -> (String) applicationKey.getId()).collect(Collectors.toSet());
          bulkSetAccountId(account.getUuid(), Command.class, appIdSet);
        }
      }
    }
  }
}
