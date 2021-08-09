package software.wings.service.intfc.account;

import software.wings.beans.Account;

public interface AccountCrudObserver {
  void onAccountCreated(Account account);
  void onAccountUpdated(Account account);
}
