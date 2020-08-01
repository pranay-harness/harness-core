package software.wings.service.impl;

import com.google.inject.Inject;

import io.harness.ccm.license.CeLicenseInfo;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;

public class AccountDao {
  @Inject private HPersistence persistence;

  public String save(Account account) {
    return persistence.save(account);
  }

  public void updateCeLicense(String accountId, CeLicenseInfo ceLicenseInfo) {
    Query query = persistence.createQuery(Account.class).field(AccountKeys.uuid).equal(accountId);
    UpdateOperations<Account> updateOperations =
        persistence.createUpdateOperations(Account.class).set(AccountKeys.ceLicenseInfo, ceLicenseInfo);
    persistence.update(query, updateOperations);
  }

  public Account get(String accountId) {
    return persistence.get(Account.class, accountId);
  }
}
