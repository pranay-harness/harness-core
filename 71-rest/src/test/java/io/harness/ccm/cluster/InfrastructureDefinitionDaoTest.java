package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;

import java.util.List;

public class InfrastructureDefinitionDaoTest extends WingsBaseTest {
  @Inject private HPersistence persistence;
  private InfrastructureDefinitionDao infrastructureDefinitionDao;

  @Before
  public void setUp() {
    infrastructureDefinitionDao = new InfrastructureDefinitionDao(persistence);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldListByCloudProviderId() {
    String cloudProviderId = "CLOUD_PROVIDER_ID";
    String clusterName = "CLUSTER_NAME";
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("NAME")
            .appId("APP_ID")
            .envId("ENV_ID")
            .deploymentType(DeploymentType.ECS)
            .cloudProviderType(CloudProviderType.AWS)
            .infrastructure(
                AwsEcsInfrastructure.builder().cloudProviderId(cloudProviderId).clusterName(clusterName).build())
            .build();
    String id = persistence.save(infrastructureDefinition);
    List<InfrastructureDefinition> infrastructureDefinitions = infrastructureDefinitionDao.list(cloudProviderId);
    assertThat(infrastructureDefinitions)
        .hasSize(1)
        .first()
        .isEqualToIgnoringGivenFields(infrastructureDefinition, InfrastructureDefinitionKeys.uuid);
  }
}
