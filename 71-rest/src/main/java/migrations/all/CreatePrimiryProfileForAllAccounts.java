package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HKeyIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateProfileService;

@Slf4j
public class CreatePrimiryProfileForAllAccounts implements Migration {
  @Inject private DelegateProfileService delegateProfileService;
  @Inject private WingsPersistence wingsPersistence;
  @Override
  public void migrate() {
    logger.info("Starting Migration");
    try (HKeyIterator<Account> keys = new HKeyIterator(wingsPersistence.createQuery(Account.class).fetchKeys())) {
      while (keys.hasNext()) {
        String accountId = keys.next().getId().toString();
        delegateProfileService.fetchPrimaryProfile(accountId);
      }
    }
  }
}
