package software.wings.service.intfc.account;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Account;

@TargetModule(HarnessModule._945_ACCOUNT_MGMT)
@OwnedBy(DEL)
public interface AccountCrudObserver {
  void onAccountCreated(Account account);
  void onAccountUpdated(Account account);
}
