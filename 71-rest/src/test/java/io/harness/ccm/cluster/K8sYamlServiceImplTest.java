package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.K8sYamlDao;
import io.harness.ccm.cluster.entities.K8sYaml;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

public class K8sYamlServiceImplTest extends WingsBaseTest {
  @Mock private K8sYamlDao k8sYamlDao;
  @Inject @InjectMocks K8sYamlServiceImpl k8sYamlService;
  private static final String ACCOUNT_ID = "accountId";
  private static final String CLUSTER_ID = "clusterId";
  private static final String RESOURCE_VERSION = "kind";
  private static final String YAML = "yaml";
  private static final String UID = "uid";
  private static final String UUID = "UUID";

  @Before
  public void setUp() {
    when(k8sYamlDao.getYaml(ACCOUNT_ID, UUID)).thenReturn(getTestYamlRecord(UID, YAML));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void shouldGetYaml() {
    K8sYaml yamlRecord = k8sYamlService.get(ACCOUNT_ID, UUID);
    assertThat(yamlRecord.getUid()).isEqualTo(UID);
    assertThat(yamlRecord.getUuid()).isEqualTo(UUID);
    assertThat(yamlRecord.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(yamlRecord.getYaml()).isEqualTo(YAML);
  }

  private K8sYaml getTestYamlRecord(String uid, String yaml) {
    return K8sYaml.builder()
        .accountId(ACCOUNT_ID)
        .clusterId(CLUSTER_ID)
        .resourceVersion(RESOURCE_VERSION)
        .uid(uid)
        .uuid(UUID)
        .yaml(yaml)
        .build();
  }
}
