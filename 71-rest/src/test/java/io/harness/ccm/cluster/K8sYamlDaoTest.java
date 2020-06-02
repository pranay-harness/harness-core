package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.K8sYamlDao;
import io.harness.ccm.cluster.entities.K8sYaml;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class K8sYamlDaoTest extends WingsBaseTest {
  @Inject private K8sYamlDao k8sYamlDao;
  private static final String ACCOUNT_ID = "accountId";
  private static final String CLUSTER_ID = "clusterId";
  private static final String RESOURCE_VERSION = "12345";
  private static final String YAML = "yaml";
  private static final String UID = "uid";
  private String hash;

  @Before
  public void setUp() {
    hash = k8sYamlDao.ensureYamlSaved(ACCOUNT_ID, CLUSTER_ID, UID, RESOURCE_VERSION, YAML);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldGetYaml() {
    K8sYaml yamlRecord = k8sYamlDao.getYaml(ACCOUNT_ID, hash);
    assertThat(yamlRecord.getUid()).isEqualTo(UID);
    assertThat(yamlRecord.getHash()).isEqualTo(hash);
    assertThat(yamlRecord.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(yamlRecord.getYaml()).isEqualTo(YAML);
  }
}
