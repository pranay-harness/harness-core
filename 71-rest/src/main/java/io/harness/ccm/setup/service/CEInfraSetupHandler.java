package io.harness.ccm.setup.service;

import com.google.inject.Inject;

import io.harness.ccm.setup.CECloudAccountDao;
import io.harness.ccm.setup.CEClusterDao;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CECloudAccount;
import software.wings.beans.ce.CECloudAccount.AccountStatus;
import software.wings.beans.ce.CECluster;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class CEInfraSetupHandler {
  @Inject protected CECloudAccountDao ceCloudAccountDao;
  @Inject protected CEClusterDao ceClusterDao;

  public abstract void syncCEInfra(SettingAttribute settingAttribute);

  public abstract void syncCEClusters(CECloudAccount ceCloudAccount);

  public abstract boolean updateAccountPermission(CECloudAccount ceCloudAccount);

  @Value
  private static class AccountIdentifierKey {
    String accountId;
    String infraAccountId;
    String infraMasterAccountId;
  }

  @Value
  private static class ClusterIdentifierKey {
    String accountId;
    String infraAccountId;
    String clusterName;
    String region;
  }

  protected void updateLinkedAccounts(
      String accountId, String settingId, String infraMasterAccountId, List<CECloudAccount> infraAccounts) {
    Map<AccountIdentifierKey, CECloudAccount> infraAccountMap = createAccountMap(infraAccounts);

    List<CECloudAccount> ceExistingAccounts =
        ceCloudAccountDao.getByMasterAccountId(accountId, settingId, infraMasterAccountId);
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

  protected void updateClusters(String accountId, String infraAccountId, List<CECluster> infraClusters) {
    Map<ClusterIdentifierKey, CECluster> infraClusterMap = createClusterMap(infraClusters);

    List<CECluster> ceExistingClusters = ceClusterDao.getByInfraAccountId(accountId, infraAccountId);
    Map<ClusterIdentifierKey, CECluster> ceExistingClusterMap = createClusterMap(ceExistingClusters);

    infraClusterMap.forEach((clusterIdentifierKey, ceCluster) -> {
      if (!ceExistingClusterMap.containsKey(clusterIdentifierKey)) {
        ceClusterDao.create(ceCluster);
      }
    });

    ceExistingClusterMap.forEach((clusterIdentifierKey, ceCluster) -> {
      if (!infraClusterMap.containsKey(clusterIdentifierKey)) {
        ceClusterDao.deleteCluster(ceCluster.getUuid());
      }
    });
  }

  protected void updateAccountStatus(CECloudAccount ceCloudAccount, boolean verifyAccess) {
    AccountStatus accountStatus = AccountStatus.NOT_CONNECTED;
    if (verifyAccess) {
      accountStatus = AccountStatus.CONNECTED;
    }
    ceCloudAccountDao.updateAccountStatus(ceCloudAccount, accountStatus);
  }

  private Map<AccountIdentifierKey, CECloudAccount> createAccountMap(List<CECloudAccount> cloudAccounts) {
    return cloudAccounts.stream().collect(Collectors.toMap(cloudAccount
        -> new AccountIdentifierKey(
            cloudAccount.getAccountId(), cloudAccount.getInfraAccountId(), cloudAccount.getInfraMasterAccountId()),
        Function.identity()));
  }

  private Map<ClusterIdentifierKey, CECluster> createClusterMap(List<CECluster> ceClusters) {
    return ceClusters.stream().collect(Collectors.toMap(ceCluster
        -> new ClusterIdentifierKey(
            ceCluster.getAccountId(), ceCluster.getInfraAccountId(), ceCluster.getClusterName(), ceCluster.getRegion()),
        Function.identity()));
  }
}
