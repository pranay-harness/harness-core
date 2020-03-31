package io.harness.ccm.setup.service;

import com.google.inject.Inject;

import io.harness.ccm.setup.dao.CECloudAccountDao;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CECloudAccount;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class CEInfraSetupHandler {
  @Inject protected CECloudAccountDao ceCloudAccountDao;

  public abstract void syncCEInfra(SettingAttribute settingAttribute);

  @Value
  private static class AccountIdentifierKey {
    String accountId;
    String infraAccountId;
    String infraMasterAccountId;
  }

  protected void updateLinkedAccounts(
      String accountId, String infraMasterAccountId, List<CECloudAccount> infraAccounts) {
    Map<AccountIdentifierKey, CECloudAccount> infraAccountMap = createAccountMap(infraAccounts);

    List<CECloudAccount> ceExistingAccounts = ceCloudAccountDao.getByMasterAccountId(accountId, infraMasterAccountId);
    Map<AccountIdentifierKey, CECloudAccount> ceExistingAccountMap = createAccountMap(ceExistingAccounts);

    infraAccountMap.forEach((accountIdentifierKey, ceCloudAccount) -> {
      if (!ceExistingAccountMap.containsKey(accountIdentifierKey)) {
        ceCloudAccountDao.create(ceCloudAccount);
      }
    });

    ceExistingAccountMap.forEach((accountIdentifierKey, ceCloudAccount) -> {
      if (!infraAccountMap.containsKey(accountIdentifierKey)) {
        ceCloudAccountDao.deleteAccount(ceCloudAccount.getUuid());
      }
    });
  }

  private Map<AccountIdentifierKey, CECloudAccount> createAccountMap(List<CECloudAccount> cloudAccounts) {
    return cloudAccounts.stream().collect(Collectors.toMap(cloudAccount
        -> new AccountIdentifierKey(
            cloudAccount.getAccountId(), cloudAccount.getInfraAccountId(), cloudAccount.getInfraMasterAccountId()),
        Function.identity()));
  }
}
