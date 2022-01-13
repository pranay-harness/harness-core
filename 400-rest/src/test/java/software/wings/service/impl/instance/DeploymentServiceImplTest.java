/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentSummary;
import software.wings.api.K8sDeploymentInfo;
import software.wings.beans.container.Label;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.CustomDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.K8sDeploymentKey;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class DeploymentServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private DeploymentServiceImpl deploymentService;

  private final String ACCOUNT_ID = "account_id";
  private final String RELEASE_NAME = "release_name";
  private final String HELM_RELEASE_NAME = "release-name";
  private final String ECS_SERVICE_NAME = "ecs_service_name";
  private final String ECS_CLUSTER_NAME = "ecs_cluster_name";
  private final String INFRA_MAPPING_ID_ECS = "infra_mapping_id_ecs";
  private final String INFRA_MAPPING_ID_K8S = "infra_mapping_id_k8s";
  private final String INFRA_MAPPING_ID_CUSTOM = "infra_mapping_id_custom";

  private final Instant NOW = Instant.now();
  private final Instant START_TIME = NOW.minus(1, ChronoUnit.DAYS);
  private final Instant END_TIME = NOW.plus(1, ChronoUnit.DAYS);

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetDeploymentSummaryForEcs() {
    DeploymentSummary ecsDeploymentSummary = getEcsDeploymentSummary();
    deploymentService.save(ecsDeploymentSummary);
    Optional<DeploymentSummary> savedDeploymentSummary = deploymentService.getWithAccountId(ecsDeploymentSummary);
    assertThat(savedDeploymentSummary).isPresent();
    assertThat(savedDeploymentSummary).map(DeploymentSummary::getInfraMappingId).hasValue(INFRA_MAPPING_ID_ECS);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetDeploymentSummaryForK8s() {
    DeploymentSummary deploymentSummary = getK8sDeploymentSummary();
    deploymentService.save(deploymentSummary);
    Optional<DeploymentSummary> savedK8sDeploymentSummary = deploymentService.getWithAccountId(deploymentSummary);
    assertThat(savedK8sDeploymentSummary).isPresent();
    assertThat(savedK8sDeploymentSummary).map(DeploymentSummary::getInfraMappingId).hasValue(INFRA_MAPPING_ID_K8S);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetDeploymentSummaryForHelm() {
    DeploymentSummary deploymentSummary = getHelmDeploymentSummary();
    deploymentService.save(deploymentSummary);
    Optional<DeploymentSummary> savedK8sDeploymentSummary = deploymentService.getWithAccountId(deploymentSummary);
    assertThat(savedK8sDeploymentSummary).isPresent();
    assertThat(savedK8sDeploymentSummary).map(DeploymentSummary::getInfraMappingId).hasValue(INFRA_MAPPING_ID_K8S);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetDeploymentSummaryForCustomDeployment() {
    DeploymentSummary deploymentSummary = getCustomDeploymentSummary();
    deploymentService.save(deploymentSummary);
    Optional<DeploymentSummary> savedK8sDeploymentSummary = deploymentService.getWithAccountId(deploymentSummary);
    assertThat(savedK8sDeploymentSummary).isPresent();
    assertThat(savedK8sDeploymentSummary).map(DeploymentSummary::getInfraMappingId).hasValue(INFRA_MAPPING_ID_CUSTOM);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetDeploymentSummaryForInfraMappingId() {
    DeploymentSummary deploymentSummary = getHelmDeploymentSummary();
    deploymentService.save(deploymentSummary);
    Optional<DeploymentSummary> savedK8sDeploymentSummary =
        deploymentService.getWithInfraMappingId(ACCOUNT_ID, INFRA_MAPPING_ID_K8S);
    assertThat(savedK8sDeploymentSummary).isPresent();
    assertThat(savedK8sDeploymentSummary).map(DeploymentSummary::getInfraMappingId).hasValue(INFRA_MAPPING_ID_K8S);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetDeploymentSummaryPaginated() {
    DeploymentSummary deploymentSummary = getK8sDeploymentSummary();
    deploymentService.save(deploymentSummary);
    DeploymentSummary ecsDeploymentSummary = getEcsDeploymentSummary();
    deploymentService.save(ecsDeploymentSummary);
    List<DeploymentSummary> deploymentSummaries =
        deploymentService.getDeploymentSummary(ACCOUNT_ID, String.valueOf(0), START_TIME, END_TIME);
    assertThat(deploymentSummaries).hasSize(2);
    assertThat(deploymentSummaries.get(0).getInfraMappingId()).isEqualTo(INFRA_MAPPING_ID_K8S);
    assertThat(deploymentSummaries.get(1).getInfraMappingId()).isEqualTo(INFRA_MAPPING_ID_ECS);
  }

  private DeploymentSummary getK8sDeploymentSummary() {
    K8sDeploymentKey k8sDeploymentKey = K8sDeploymentKey.builder().releaseName(RELEASE_NAME).build();
    K8sDeploymentInfo k8sDeploymentInfo = K8sDeploymentInfo.builder().releaseName(RELEASE_NAME).build();
    return DeploymentSummary.builder()
        .accountId(ACCOUNT_ID)
        .k8sDeploymentKey(k8sDeploymentKey)
        .deploymentInfo(k8sDeploymentInfo)
        .infraMappingId(INFRA_MAPPING_ID_K8S)
        .build();
  }

  private DeploymentSummary getHelmDeploymentSummary() {
    Label label = Label.Builder.aLabel().withName(HELM_RELEASE_NAME).withValue(HELM_RELEASE_NAME).build();
    ContainerDeploymentKey containerDeploymentKey =
        ContainerDeploymentKey.builder().labels(Arrays.asList(label)).build();
    return DeploymentSummary.builder()
        .accountId(ACCOUNT_ID)
        .containerDeploymentKey(containerDeploymentKey)
        .infraMappingId(INFRA_MAPPING_ID_K8S)
        .build();
  }

  private DeploymentSummary getCustomDeploymentSummary() {
    return DeploymentSummary.builder()
        .infraMappingId(INFRA_MAPPING_ID_CUSTOM)
        .customDeploymentKey(
            CustomDeploymentKey.builder().instanceFetchScriptHash(1234).tags(Arrays.asList("foo", "bar")).build())
        .build();
  }

  private DeploymentSummary getEcsDeploymentSummary() {
    ContainerDeploymentKey containerDeploymentKey =
        ContainerDeploymentKey.builder().containerServiceName(ECS_SERVICE_NAME).build();
    ContainerDeploymentInfoWithNames containerDeploymentInfoWithNames = ContainerDeploymentInfoWithNames.builder()
                                                                            .clusterName(ECS_CLUSTER_NAME)
                                                                            .containerSvcName(ECS_SERVICE_NAME)
                                                                            .build();
    return DeploymentSummary.builder()
        .accountId(ACCOUNT_ID)
        .containerDeploymentKey(containerDeploymentKey)
        .deploymentInfo(containerDeploymentInfoWithNames)
        .infraMappingId(INFRA_MAPPING_ID_ECS)
        .build();
  }
}
