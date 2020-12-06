package migrations.all;

import io.harness.persistence.HKeyIterator;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateProfileService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;

@Slf4j
public class CreatePrimiryProfileForAllAccounts implements Migration {
  @Inject private DelegateProfileService delegateProfileService;
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    log.info("Starting Migration");
    try (HKeyIterator<Account> keys = new HKeyIterator(wingsPersistence.createQuery(Account.class).fetchKeys())) {
      while (keys.hasNext()) {
        String accountId = keys.next().getId().toString();
        delegateProfileService.fetchPrimaryProfile(accountId);
      }
    }
  }
}
