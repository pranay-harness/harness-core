package software.wings.service.impl.instance;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentSummary;
import software.wings.api.K8sDeploymentInfo;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.K8sDeploymentKey;

import java.util.Optional;

public class DeploymentServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private DeploymentServiceImpl deploymentService;

  private final String ACCOUNT_ID = "account_id";
  private final String RELEASE_NAME = "release_name";
  private final String ECS_SERVICE_NAME = "ecs_service_name";
  private final String ECS_CLUSTER_NAME = "ecs_cluster_name";
  private final String INFRA_MAPPING_ID = "infra_mapping_id";

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetDeploymentSummaryForEcs() {
    ContainerDeploymentKey containerDeploymentKey =
        ContainerDeploymentKey.builder().containerServiceName(ECS_SERVICE_NAME).build();
    ContainerDeploymentInfoWithNames containerDeploymentInfoWithNames = ContainerDeploymentInfoWithNames.builder()
                                                                            .clusterName(ECS_CLUSTER_NAME)
                                                                            .containerSvcName(ECS_SERVICE_NAME)
                                                                            .build();
    DeploymentSummary deploymentSummary = DeploymentSummary.builder()
                                              .accountId(ACCOUNT_ID)
                                              .containerDeploymentKey(containerDeploymentKey)
                                              .deploymentInfo(containerDeploymentInfoWithNames)
                                              .infraMappingId(INFRA_MAPPING_ID)
                                              .build();
    deploymentService.save(deploymentSummary);
    Optional<DeploymentSummary> savedDeploymentSummary = deploymentService.getWithAccountId(deploymentSummary);
    assertThat(savedDeploymentSummary).isPresent();
    assertThat(savedDeploymentSummary).map(DeploymentSummary::getInfraMappingId).hasValue(INFRA_MAPPING_ID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetDeploymentSummaryForK8s() {
    K8sDeploymentKey k8sDeploymentKey = K8sDeploymentKey.builder().releaseName(RELEASE_NAME).build();
    K8sDeploymentInfo k8sDeploymentInfo = K8sDeploymentInfo.builder().releaseName(RELEASE_NAME).build();
    DeploymentSummary deploymentSummary = DeploymentSummary.builder()
                                              .accountId(ACCOUNT_ID)
                                              .k8sDeploymentKey(k8sDeploymentKey)
                                              .deploymentInfo(k8sDeploymentInfo)
                                              .infraMappingId(INFRA_MAPPING_ID)
                                              .build();
    deploymentService.save(deploymentSummary);
    Optional<DeploymentSummary> savedK8sDeploymentSummary = deploymentService.getWithAccountId(deploymentSummary);
    assertThat(savedK8sDeploymentSummary).isPresent();
    assertThat(savedK8sDeploymentSummary).map(DeploymentSummary::getInfraMappingId).hasValue(INFRA_MAPPING_ID);
  }
}
