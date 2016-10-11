package software.wings.service.impl;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
@Singleton
@ValidateOnExecution
public class AccountServiceImpl implements AccountService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public Account save(@Valid Account account) {
    wingsPersistence.save(account);
    return account;
  }

  @Override
  public Account get(String accountId) {
    return wingsPersistence.get(Account.class, accountId);
  }

  @Override
  public void delete(String accountId) {
    wingsPersistence.delete(Account.class, accountId);
  }

  @Override
  public Account update(@Valid Account account) {
    wingsPersistence.update(
        account, wingsPersistence.createUpdateOperations(Account.class).set("companyName", account.getCompanyName()));
    return wingsPersistence.get(Account.class, account.getUuid());
  }

  @Override
  public Account getByName(String companyName) {
    return wingsPersistence.executeGetOneQuery(
        wingsPersistence.createQuery(Account.class).field("companyName").equal(companyName));
  }
}
