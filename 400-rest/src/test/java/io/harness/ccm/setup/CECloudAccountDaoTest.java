/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.setup;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.entities.billing.CECloudAccount;
import io.harness.ccm.commons.entities.billing.CECloudAccount.AccountStatus;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CE)
public class CECloudAccountDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String accountName = "ACCOUNT_NAME";
  private String infraAccountId = "123123112";
  private String infraMasterAccountId = "3243223122";
  private String masterAccountSettingId = "MASTER_SETTING_ID";
  private String accountArn = "arn:aws:organizations::123123112:account/o-tbm3caqef8/3243223122";
  @Inject private CECloudAccountDao ceCloudAccountDao;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnAccountForMasterAccountId() {
    boolean savedAccount = ceCloudAccountDao.create(getCECloudAccount());
    assertThat(savedAccount).isTrue();
    List<CECloudAccount> ceCloudAccounts =
        ceCloudAccountDao.getByMasterAccountId(accountId, masterAccountSettingId, infraMasterAccountId);
    CECloudAccount savedCECloudAccount = ceCloudAccounts.get(0);
    assertThat(savedCECloudAccount.getAccountId()).isEqualTo(accountId);
    assertThat(savedCECloudAccount.getAccountName()).isEqualTo(accountName);
    assertThat(savedCECloudAccount.getAccountArn()).isEqualTo(accountArn);
    assertThat(savedCECloudAccount.getInfraAccountId()).isEqualTo(infraAccountId);
    assertThat(savedCECloudAccount.getInfraMasterAccountId()).isEqualTo(infraMasterAccountId);
    assertThat(savedCECloudAccount.getMasterAccountSettingId()).isEqualTo(masterAccountSettingId);
  }

  private CECloudAccount getCECloudAccount() {
    return CECloudAccount.builder()
        .accountId(accountId)
        .accountName(accountName)
        .accountArn(accountArn)
        .infraAccountId(infraAccountId)
        .infraMasterAccountId(infraMasterAccountId)
        .masterAccountSettingId(masterAccountSettingId)
        .build();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldDeleteCECloudAccount() {
    ceCloudAccountDao.create(getCECloudAccount());
    List<CECloudAccount> ceCloudAccounts =
        ceCloudAccountDao.getByMasterAccountId(accountId, masterAccountSettingId, infraMasterAccountId);
    CECloudAccount savedCECloudAccount = ceCloudAccounts.get(0);
    ceCloudAccountDao.deleteAccount(savedCECloudAccount.getUuid());
    List<CECloudAccount> ceCloudAccountList =
        ceCloudAccountDao.getByMasterAccountId(accountId, masterAccountSettingId, infraMasterAccountId);
    assertThat(ceCloudAccountList).hasSize(0);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldUpdateCECloudAccountStatus() {
    AccountStatus accountStatus = AccountStatus.CONNECTED;
    ceCloudAccountDao.create(getCECloudAccount());
    List<CECloudAccount> ceCloudAccounts =
        ceCloudAccountDao.getByMasterAccountId(accountId, masterAccountSettingId, infraMasterAccountId);
    CECloudAccount savedCECloudAccount = ceCloudAccounts.get(0);
    ceCloudAccountDao.updateAccountStatus(savedCECloudAccount, accountStatus);
    List<CECloudAccount> ceCloudAccountList =
        ceCloudAccountDao.getByMasterAccountId(accountId, masterAccountSettingId, infraMasterAccountId);
    CECloudAccount ceCloudAccount = ceCloudAccountList.get(0);
    assertThat(ceCloudAccount.getAccountStatus()).isEqualTo(accountStatus);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldFetchAccountByInfraAccountId() {
    ceCloudAccountDao.create(getCECloudAccount());
    List<CECloudAccount> ceCloudAccountsByInfraAccId = ceCloudAccountDao.getByAWSAccountId(accountId);
    assertThat(ceCloudAccountsByInfraAccId.get(0).getAccountName()).isEqualTo(accountName);
  }
}
