package io.harness.ccm.setup.dao;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.ce.CECluster;

import java.util.List;

public class CEClusterDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String infraAccountId = "123123112";
  private String infraMasterAccountId = "3243223122";
  private String masterAccountSettingId = "MASTER_SETTING_ID";
  private String clusterName = "EKS_CLUSTER";
  private String region = "us-east-1";
  @Inject private CEClusterDao ceClusterDao;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnClusterForInfraAccountId() {
    boolean savedAccount = ceClusterDao.create(getCECluster());
    assertThat(savedAccount).isTrue();
    List<CECluster> ceClusters = ceClusterDao.getByInfraAccountId(accountId, infraAccountId);
    CECluster savedCECluster = ceClusters.get(0);
    assertThat(savedCECluster.getAccountId()).isEqualTo(accountId);
    assertThat(savedCECluster.getClusterName()).isEqualTo(clusterName);
    assertThat(savedCECluster.getRegion()).isEqualTo(region);
    assertThat(savedCECluster.getInfraAccountId()).isEqualTo(infraAccountId);
    assertThat(savedCECluster.getInfraMasterAccountId()).isEqualTo(infraMasterAccountId);
    assertThat(savedCECluster.getParentAccountSettingId()).isEqualTo(masterAccountSettingId);
  }

  private CECluster getCECluster() {
    return CECluster.builder()
        .accountId(accountId)
        .infraAccountId(infraAccountId)
        .infraMasterAccountId(infraMasterAccountId)
        .parentAccountSettingId(masterAccountSettingId)
        .clusterName(clusterName)
        .region(region)
        .build();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldDeleteCECloudAccount() {
    ceClusterDao.create(getCECluster());
    List<CECluster> ceClusters = ceClusterDao.getByInfraAccountId(accountId, infraAccountId);
    CECluster savedCECluster = ceClusters.get(0);
    ceClusterDao.deleteCluster(savedCECluster.getUuid());
    List<CECluster> ceClusterList = ceClusterDao.getByInfraAccountId(accountId, infraAccountId);
    assertThat(ceClusterList).hasSize(0);
  }
}
