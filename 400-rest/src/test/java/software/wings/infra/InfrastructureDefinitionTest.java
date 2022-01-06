/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.YOGESH;

import static software.wings.api.DeploymentType.CUSTOM;
import static software.wings.infra.InfraDefinitionTestConstants.RELEASE_NAME;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.CustomInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.utils.WingsTestConstants;

import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InfrastructureDefinitionTest extends WingsBaseTest {
  private InfraMappingInfrastructureProvider mappingProvider;

  private InfrastructureDefinition infrastructureDefinition;

  @After
  public void tearDown() throws Exception {
    mappingProvider = null;
    infrastructureDefinition = null;
  }

  @Before
  public void setUp() throws Exception {
    mappingProvider = GoogleKubernetesEngine.builder()
                          .clusterName(WingsTestConstants.CLUSTER_NAME)
                          .cloudProviderId(WingsTestConstants.COMPUTE_PROVIDER_ID)
                          .namespace(WingsTestConstants.NAMESPACE)
                          .releaseName(RELEASE_NAME)
                          .build();

    infrastructureDefinition = InfrastructureDefinition.builder()
                                   .infrastructure(mappingProvider)
                                   .appId(WingsTestConstants.APP_ID)
                                   .cloudProviderType(CloudProviderType.GCP)
                                   .deploymentType(DeploymentType.KUBERNETES)
                                   .envId(WingsTestConstants.ENV_ID)
                                   .name("GCPInfra")
                                   .build();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testGetInfraMapping() {
    InfrastructureMapping infrastructureMapping = infrastructureDefinition.getInfraMapping();
    assertThat(infrastructureDefinition.getInfrastructure().getMappingClass())
        .isEqualTo(infrastructureMapping.getClass());

    GcpKubernetesInfrastructureMapping infraMapping = (GcpKubernetesInfrastructureMapping) infrastructureMapping;

    assertThat(RELEASE_NAME).isEqualTo(infraMapping.getReleaseName());
    assertThat(WingsTestConstants.NAMESPACE).isEqualTo(infraMapping.getNamespace());
    assertThat(WingsTestConstants.COMPUTE_PROVIDER_ID).isEqualTo(infraMapping.getComputeProviderSettingId());
    assertThat(WingsTestConstants.CLUSTER_NAME).isEqualTo(infraMapping.getClusterName());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetInfraMappingForCustomInfra() {
    InfrastructureDefinition infraDef =
        InfrastructureDefinition.builder()
            .infrastructure(CustomInfrastructure.builder().deploymentTypeTemplateVersion("1.1").build())
            .deploymentType(CUSTOM)
            .cloudProviderType(CloudProviderType.CUSTOM)
            .build()
            .cloneForUpdate();
    infraDef.setDeploymentTypeTemplateId(WingsTestConstants.TEMPLATE_ID);

    InfrastructureMapping infrastructureMapping = infraDef.getInfraMapping();
    assertThat(infraDef.getInfrastructure().getMappingClass()).isEqualTo(infrastructureMapping.getClass());

    CustomInfrastructureMapping infraMapping = (CustomInfrastructureMapping) infrastructureMapping;
    assertThat(infraMapping.getCustomDeploymentTemplateId()).isEqualTo(WingsTestConstants.TEMPLATE_ID);
    assertThat(infraMapping.getDeploymentTypeTemplateVersion()).isEqualTo("1.1");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testCloneForUpdate() {
    final InfrastructureDefinition source =
        InfrastructureDefinition.builder()
            .name("my-infra")
            .provisionerId(WingsTestConstants.PROVISIONER_ID)
            .cloudProviderType(CloudProviderType.CUSTOM)
            .deploymentType(CUSTOM)
            .infrastructure(CustomInfrastructure.builder().deploymentTypeTemplateVersion("1.1").build())
            .customDeploymentName("my-deployment")
            .deploymentTypeTemplateId("my-deployment-id")
            .appId(WingsTestConstants.APP_ID)
            .accountId(WingsTestConstants.ACCOUNT_ID)
            .scopedToServices(Arrays.asList(WingsTestConstants.SERVICE_ID))
            .build();

    final InfrastructureDefinition target = source.cloneForUpdate();

    assertThat(target.getName()).isEqualTo("my-infra");
    assertThat(target.getProvisionerId()).isEqualTo(WingsTestConstants.PROVISIONER_ID);
    assertThat(target.getCloudProviderType()).isEqualTo(CloudProviderType.CUSTOM);
    assertThat(target.getDeploymentType()).isEqualTo(CUSTOM);
    assertThat(target.getInfrastructure())
        .isEqualTo(CustomInfrastructure.builder().deploymentTypeTemplateVersion("1.1").build());
    assertThat(target.getCustomDeploymentName()).isEqualTo("my-deployment");
    assertThat(target.getDeploymentTypeTemplateId()).isEqualTo("my-deployment-id");
    assertThat(target.getAccountId()).isEqualTo(WingsTestConstants.ACCOUNT_ID);
    assertThat(target.getScopedToServices()).containsExactly(WingsTestConstants.SERVICE_ID);
  }
}
