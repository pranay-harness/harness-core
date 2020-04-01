package io.harness.ccm.setup.service.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.setup.dao.CECloudAccountDao;
import io.harness.ccm.setup.dao.CEClusterDao;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.ccm.setup.service.intfc.AwsEKSClusterService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;
import software.wings.beans.ce.CECluster;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AwsCEInfraSetupHandlerTest extends CategoryTest {
  @InjectMocks private AwsCEInfraSetupHandler awsCEInfraSetupHandler;
  @Mock private CECloudAccountDao ceCloudAccountDao;
  @Mock private CEClusterDao ceClusterDao;
  @Mock private AWSAccountService awsAccountService;
  @Mock private AwsEKSClusterService awsEKSClusterService;

  @Captor private ArgumentCaptor<CECloudAccount> ceCloudCreateAccountArgumentCaptor;
  @Captor private ArgumentCaptor<String> ceCloudDeleteAccountArgumentCaptor;

  @Captor private ArgumentCaptor<CECluster> ceCreateClusterArgumentCaptor;
  @Captor private ArgumentCaptor<String> ceDeleteClusterArgumentCaptor;

  private String accountId = "ACCOUNT_ID";
  private String deleteRecordUUID = "DELETE_RECORD_UUID";
  private String accountName = "ACCOUNT_NAME";
  private String accountNameDelete = "ACCOUNT_NAME_DELETE";
  private String infraAccountId = "123123112";
  private String infraAccountIdDelete = "4423232112";
  private String infraMasterAccountId = "3243223122";
  private String masterAccountSettingId = "MASTER_SETTING_ID";
  private String accountArn = "arn:aws:organizations::123123112:account/o-tbm3caqef8/3243223122";
  private String accountArnDelete = "arn:aws:organizations::123123112:account/o-tbm3caqef8/4423232112";
  private String clusterName = "CLUSTER_NAME";
  private String clusterNameCreate = "CLUSTER_NAME_CREATE";
  private String clusterNameDelete = "CLUSTER_NAME_DELETE";
  private String region = "US_EAST_1";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testSyncInfra() {
    CEAwsConfig ceAwsConfig =
        CEAwsConfig.builder()
            .awsAccountId(infraMasterAccountId)
            .awsMasterAccountId(infraMasterAccountId)
            .awsAccountType(CEAwsConfig.AWSAccountType.MASTER_ACCOUNT.name())
            .awsCrossAccountAttributes(AwsCrossAccountAttributes.builder()
                                           .crossAccountRoleArn("arn:aws:iam::454324243:role/harness_master_account")
                                           .externalId("externalId")
                                           .build())
            .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withUuid(masterAccountSettingId)
                                            .withAccountId(accountId)
                                            .withCategory(SettingAttribute.SettingCategory.CE_CONNECTOR)
                                            .withValue(ceAwsConfig)
                                            .build();

    CECloudAccount ceCloudAccountSaved = getCECloudAccount(accountNameDelete, accountArnDelete, infraAccountIdDelete);
    ceCloudAccountSaved.setUuid(deleteRecordUUID);
    CECloudAccount ceCloudAccount = getCECloudAccount(accountName, accountArn, infraAccountId);
    List<CECloudAccount> savedCEAccounts = ImmutableList.of(ceCloudAccountSaved);
    List<CECloudAccount> infraAccounts = ImmutableList.of(ceCloudAccount);

    CECluster ceCluster = getCECluster(clusterName, region);
    CECluster ceClusterCreate = getCECluster(clusterNameCreate, region);
    CECluster ceClusterDelete = getCECluster(clusterNameDelete, region);
    ceClusterDelete.setUuid(deleteRecordUUID);

    List<CECluster> savedCEClusters = ImmutableList.of(ceCluster, ceClusterDelete);
    List<CECluster> infraClusters = ImmutableList.of(ceCluster, ceClusterCreate);

    when(ceCloudAccountDao.getByMasterAccountId(accountId, infraMasterAccountId)).thenReturn(savedCEAccounts);
    when(awsAccountService.getAWSAccounts(accountId, masterAccountSettingId, ceAwsConfig)).thenReturn(infraAccounts);

    when(ceClusterDao.getByInfraAccountId(accountId, infraMasterAccountId)).thenReturn(savedCEClusters);
    when(awsEKSClusterService.getEKSCluster(accountId, masterAccountSettingId, ceAwsConfig)).thenReturn(infraClusters);

    awsCEInfraSetupHandler.syncCEInfra(settingAttribute);
    verify(ceCloudAccountDao).create(ceCloudCreateAccountArgumentCaptor.capture());
    verify(ceCloudAccountDao).deleteAccount(ceCloudDeleteAccountArgumentCaptor.capture());
    verify(ceClusterDao).create(ceCreateClusterArgumentCaptor.capture());
    verify(ceClusterDao).deleteCluster(ceDeleteClusterArgumentCaptor.capture());
    CECloudAccount createCECloudAccount = ceCloudCreateAccountArgumentCaptor.getValue();
    String deleteCECloudAccountUUID = ceCloudDeleteAccountArgumentCaptor.getValue();
    CECluster createCECluster = ceCreateClusterArgumentCaptor.getValue();
    String deleteClusterUUID = ceDeleteClusterArgumentCaptor.getValue();
    assertThat(createCECloudAccount.getAccountArn()).isEqualTo(accountArn);
    assertThat(deleteCECloudAccountUUID).isEqualTo(deleteRecordUUID);
    assertThat(createCECluster.getClusterName()).isEqualTo(clusterNameCreate);
    assertThat(deleteClusterUUID).isEqualTo(deleteRecordUUID);
  }

  private CECloudAccount getCECloudAccount(String accountName, String accountArn, String infraAccountId) {
    return CECloudAccount.builder()
        .accountId(accountId)
        .accountName(accountName)
        .accountArn(accountArn)
        .infraAccountId(infraAccountId)
        .infraMasterAccountId(infraMasterAccountId)
        .masterAccountSettingId(masterAccountSettingId)
        .build();
  }

  private CECluster getCECluster(String clusterName, String region) {
    return CECluster.builder()
        .accountId(accountId)
        .infraAccountId(infraAccountId)
        .infraMasterAccountId(infraMasterAccountId)
        .clusterName(clusterName)
        .region(region)
        .parentAccountSettingId(masterAccountSettingId)
        .build();
  }
}
