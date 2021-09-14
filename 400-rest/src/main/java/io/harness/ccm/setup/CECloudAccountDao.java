/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.commons.entities.billing.CECloudAccount.AccountStatus;
import io.harness.ccm.commons.entities.billing.CECloudAccount.CECloudAccountKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@OwnedBy(CE)
public class CECloudAccountDao {
  private final HPersistence hPersistence;

  @Inject
  public CECloudAccountDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public boolean deleteAccount(String uuid) {
    Query<CECloudAccount> query =
        hPersistence.createQuery(CECloudAccount.class).field(CECloudAccountKeys.uuid).equal(uuid);
    return hPersistence.delete(query);
  }

  public boolean create(CECloudAccount ceCloudAccount) {
    return hPersistence.save(ceCloudAccount) != null;
  }

  public List<CECloudAccount> getByMasterAccountId(String accountId, String settingId, String infraMasterAccountId) {
    return hPersistence.createQuery(CECloudAccount.class)
        .field(CECloudAccountKeys.accountId)
        .equal(accountId)
        .field(CECloudAccountKeys.infraMasterAccountId)
        .equal(infraMasterAccountId)
        .field(CECloudAccountKeys.masterAccountSettingId)
        .equal(settingId)
        .asList();
  }

  public List<CECloudAccount> getByAWSAccountId(String harnessAccountId) {
    return hPersistence.createQuery(CECloudAccount.class)
        .field(CECloudAccountKeys.accountId)
        .equal(harnessAccountId)
        .asList();
  }

  public List<CECloudAccount> getBySettingId(String harnessAccountId, String masterAccountSettingId) {
    return hPersistence.createQuery(CECloudAccount.class)
        .field(CECloudAccountKeys.accountId)
        .equal(harnessAccountId)
        .field(CECloudAccountKeys.masterAccountSettingId)
        .equal(masterAccountSettingId)
        .asList();
  }

  public boolean updateAccountStatus(CECloudAccount ceCloudAccount, AccountStatus accountStatus) {
    UpdateOperations<CECloudAccount> updateOperations = hPersistence.createUpdateOperations(CECloudAccount.class);

    updateOperations.set(CECloudAccountKeys.accountStatus, accountStatus);
    UpdateResults updateResults = hPersistence.update(ceCloudAccount, updateOperations);
    return updateResults.getUpdatedCount() > 0;
  }
}
