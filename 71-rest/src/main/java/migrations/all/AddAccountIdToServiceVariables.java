package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.persistence.HIterator;
import org.mongodb.morphia.Key;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.ServiceVariable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AddAccountIdToServiceVariables extends AddAccountIdToAppEntities {
  @Override
  public void migrate() {
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class).project(Account.ID_KEY, true).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();

        List<Key<Application>> appIdKeyList = wingsPersistence.createQuery(Application.class)
                                                  .filter(ApplicationKeys.accountId, account.getUuid())
                                                  .asKeyList();
        if (isNotEmpty(appIdKeyList)) {
          Set<String> appIdSet =
              appIdKeyList.stream().map(applicationKey -> (String) applicationKey.getId()).collect(Collectors.toSet());
          bulkSetAccountId(account.getUuid(), ServiceVariable.class, appIdSet);
        }
      }
    }
  }
}