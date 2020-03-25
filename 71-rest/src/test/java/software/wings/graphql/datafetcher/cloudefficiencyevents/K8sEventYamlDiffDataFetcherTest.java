package software.wings.graphql.datafetcher.cloudefficiencyevents;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.K8sYamlServiceImpl;
import io.harness.ccm.cluster.entities.K8sYaml;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.query.QLK8sEventYamlDiffQueryParameters;

import java.sql.SQLException;

public class K8sEventYamlDiffDataFetcherTest extends AbstractDataFetcherTest {
  @Mock K8sYamlServiceImpl k8sYamlService;
  @Inject @InjectMocks K8sEventYamlDiffDataFetcher k8sEventYamlDiffDataFetcher;

  private static final String ACCOUNT_ID = "accountId";
  private static final String CLUSTER_ID = "clusterId";
  private static final String RESOURCE_VERSION = "kind";
  private static final String OLD_YAML = "old_yaml";
  private static final String NEW_YAML = "new_yaml";
  private static final String UID1 = "uid1";
  private static final String UID2 = "uid2";
  private static final String UUID1 = "UUID1";
  private static final String UUID2 = "UUID2";

  @Before
  public void setup() throws SQLException {
    when(k8sYamlService.get(ACCOUNT_ID, UUID1)).thenReturn(getTestYamlRecord(UID1, UUID1, OLD_YAML));
    when(k8sYamlService.get(ACCOUNT_ID, UUID2)).thenReturn(getTestYamlRecord(UID2, UUID2, NEW_YAML));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchYamlDiff() throws SQLException {
    QLK8sEventYamlDiffQueryParameters parameters = new QLK8sEventYamlDiffQueryParameters(UUID1, UUID2);
    QLK8sEventYamlDiff yamlDiff = k8sEventYamlDiffDataFetcher.fetch(parameters, ACCOUNT_ID);
    assertThat(yamlDiff).isNotNull();
    assertThat(yamlDiff.getData().getOldYaml()).isEqualTo(OLD_YAML);
    assertThat(yamlDiff.getData().getNewYaml()).isEqualTo(NEW_YAML);
  }

  private K8sYaml getTestYamlRecord(String uid, String uuid, String yaml) {
    return K8sYaml.builder()
        .accountId(ACCOUNT_ID)
        .clusterId(CLUSTER_ID)
        .resourceVersion(RESOURCE_VERSION)
        .uid(uid)
        .uuid(uuid)
        .yaml(yaml)
        .build();
  }
}
