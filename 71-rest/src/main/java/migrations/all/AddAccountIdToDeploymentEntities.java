package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class AddAccountIdToDeploymentEntities extends AddAccountIdToAppEntities {
  @Override
  public void migrate() {
    try (HIterator<Account> accounts = new HIterator<>(
             wingsPersistence.createQuery(Account.class, excludeAuthority).project(Account.ID_KEY, true).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();
        final String accountId = account.getUuid();

        List<Key<Application>> appIdKeyList =
            wingsPersistence.createQuery(Application.class).filter(ApplicationKeys.accountId, accountId).asKeyList();
        if (isNotEmpty(appIdKeyList)) {
          Set<String> appIdSet =
              appIdKeyList.stream().map(applicationKey -> (String) applicationKey.getId()).collect(Collectors.toSet());

          bulkSetAccountId(accountId, ApplicationManifest.class, appIdSet);
          bulkSetAccountId(accountId, ContainerTask.class, appIdSet);
          bulkSetAccountId(accountId, HelmChartSpecification.class, appIdSet);
          bulkSetAccountId(accountId, LambdaSpecification.class, appIdSet);
          bulkSetAccountId(accountId, ManifestFile.class, appIdSet);
          bulkSetAccountId(accountId, PcfServiceSpecification.class, appIdSet);
          bulkSetAccountId(accountId, UserDataSpecification.class, appIdSet);
        }
      }
    }
  }
}
