/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.infrastructuredefinition;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.category.element.IntegrationTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.rule.Owner;

import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure.AwsInstanceInfrastructureBuilder;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionBuilder;
import software.wings.infra.PhysicalInfra;
import software.wings.integration.IntegrationTestBase;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class InfrastructureDefinitionServiceIntegrationTest extends IntegrationTestBase {
  @InjectMocks @Inject private AppService appService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private EnvironmentService environmentService;

  private Application app1, app2;
  private Environment app1_env1, app2_env2;
  private SettingAttribute cloudProvider;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginDefaultUser();
    app1 = appService.save(anApplication().name("App1" + System.currentTimeMillis()).accountId(accountId).build());
    app2 = appService.save(anApplication().name("App2" + System.currentTimeMillis()).accountId(accountId).build());
    app1_env1 =
        environmentService.save(Environment.Builder.anEnvironment().appId(app1.getUuid()).name("app1_env_1").build());
    app2_env2 =
        environmentService.save(Environment.Builder.anEnvironment().appId(app2.getUuid()).name("app2_env_2").build());
    cloudProvider = SettingAttribute.Builder.aSettingAttribute()
                        .withUuid(SETTING_ID)
                        .withCategory(SettingCategory.CLOUD_PROVIDER)
                        .withAccountId(accountId)
                        .withName("cloud_provider")
                        .withValue(AwsConfig.builder().accountId(accountId).build())
                        .build();
    wingsPersistence.save(cloudProvider);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testListNamesByProvisionerId() {
    InfrastructureDefinition id1 = InfrastructureDefinition.builder()
                                       .appId(app1.getUuid())
                                       .provisionerId("prov_1")
                                       .name("id1")
                                       .envId(app1_env1.getUuid())
                                       .build();
    InfrastructureDefinition id2 = InfrastructureDefinition.builder()
                                       .appId(app1.getUuid())
                                       .provisionerId("prov_1")
                                       .name("id2")
                                       .envId(app1_env1.getUuid())
                                       .build();
    InfrastructureDefinition id3 =
        InfrastructureDefinition.builder().appId(app1.getUuid()).provisionerId("prov_2").name("id3").build();
    InfrastructureDefinition id4 =
        InfrastructureDefinition.builder().appId(app2.getUuid()).envId(app2_env2.getUuid()).name("id4").build();
    saveAll(id1, id2, id3, id4);
    List<String> infraDefNames = infrastructureDefinitionService.listNamesByProvisionerId(app1.getUuid(), "prov_1");
    assertThat(infraDefNames).containsExactlyInAnyOrder("id1", "id2");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testListNamesByConnectionAttr() {
    final String connectionAttr1 = "private-key-1";
    final String connectionAttr2 = "private-key-2";
    AwsInstanceInfrastructureBuilder infrastructureBuilder =
        AwsInstanceInfrastructure.builder().cloudProviderId(cloudProvider.getUuid());
    InfrastructureDefinition id1 =
        InfrastructureDefinition.builder()
            .appId(app1.getUuid())
            .envId(app1_env1.getUuid())
            .infrastructure(infrastructureBuilder.hostConnectionAttrs(connectionAttr1).build())
            .name("id1")
            .build();
    InfrastructureDefinition id2 =
        InfrastructureDefinition.builder()
            .appId(app1.getUuid())
            .envId(app1_env1.getUuid())
            .infrastructure(infrastructureBuilder.hostConnectionAttrs(connectionAttr2).build())
            .name("id_2")
            .build();
    InfrastructureDefinition id3 =
        InfrastructureDefinition.builder()
            .appId(app2.getUuid())
            .envId(app2_env2.getUuid())
            .infrastructure(infrastructureBuilder.hostConnectionAttrs(connectionAttr1).build())
            .name("id3")
            .build();
    saveAll(id1, id2, id3);
    List<String> infraDefNames = infrastructureDefinitionService.listNamesByConnectionAttr(accountId, connectionAttr1);
    assertThat(infraDefNames).containsExactlyInAnyOrder("id1", "id3");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testListNamesByComputeProviderId() {
    InfrastructureDefinitionBuilder infrastructureDefinitionBuilder =
        InfrastructureDefinition.builder().appId(app1.getUuid()).envId(app1_env1.getUuid());
    AwsInstanceInfrastructureBuilder infrastructureBuilder = AwsInstanceInfrastructure.builder();
    InfrastructureDefinition id1 =
        infrastructureDefinitionBuilder
            .infrastructure(infrastructureBuilder.cloudProviderId(cloudProvider.getUuid()).build())
            .name("id1")
            .build();
    InfrastructureDefinition id2 =
        infrastructureDefinitionBuilder.infrastructure(infrastructureBuilder.cloudProviderId("some-random-id").build())
            .name("id2")
            .build();
    infrastructureDefinitionBuilder =
        InfrastructureDefinition.builder().appId(app2.getUuid()).envId(app2_env2.getUuid());
    InfrastructureDefinition id3 =
        infrastructureDefinitionBuilder
            .infrastructure(infrastructureBuilder.cloudProviderId(cloudProvider.getUuid()).build())
            .name("id3")
            .build();
    saveAll(id1, id2, id3);
    List<String> infraDefNames =
        infrastructureDefinitionService.listNamesByComputeProviderId(accountId, cloudProvider.getUuid());
    assertThat(infraDefNames).containsExactlyInAnyOrder("id1", "id3");
  }

  private List<InfrastructureDefinition> saveAll(InfrastructureDefinition... defs) {
    List<InfrastructureDefinition> infrastructureDefinitionsCreated = new ArrayList<>();
    for (InfrastructureDefinition infrastructureDefinition : defs) {
      setRequiredFields(infrastructureDefinition);
      wingsPersistence.save(infrastructureDefinition);
      infrastructureDefinitionsCreated.add(infrastructureDefinition);
    }
    return infrastructureDefinitionsCreated;
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(IntegrationTests.class)
  public void testGetNameAndIdForEnvironment() {
    Environment env = createEnvironment("testGetNameAndIdForEnvironment");

    try {
      shouldGetWhenNoEntries(env);
      shouldLimitWhenMoreEntries(env);
    } finally {
      deleteEnvironment(env);
    }
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(IntegrationTests.class)
  public void testGetCountForEnvironments() {
    Environment env1 = createEnvironment("testGetCountForEnvironments1");
    Environment env2 = createEnvironment("testGetCountForEnvironments2");

    try {
      shouldGetZeroAndCorrectCount(env1, env2);
    } finally {
      deleteEnvironment(env1);
      deleteEnvironment(env2);
    }
  }

  private void shouldGetZeroAndCorrectCount(Environment env1, Environment env2) {
    InfrastructureDefinition infraDef1 = InfrastructureDefinition.builder()
                                             .appId(env2.getAppId())
                                             .envId(env2.getUuid())
                                             .name("infra1")
                                             .infrastructure(PhysicalInfra.builder().build())
                                             .build();
    InfrastructureDefinition infraDef2 = InfrastructureDefinition.builder()
                                             .appId(env2.getAppId())
                                             .envId(env2.getUuid())
                                             .name("infra2")
                                             .infrastructure(PhysicalInfra.builder().build())
                                             .build();
    saveAll(infraDef1, infraDef2);

    try {
      Map<String, Integer> countForEnvironments = infrastructureDefinitionService.getCountForEnvironments(
          env1.getAppId(), Arrays.asList(env1.getUuid(), env2.getUuid()));

      assertThat(countForEnvironments.get(env1.getUuid())).isEqualTo(0);
      assertThat(countForEnvironments.get(env2.getUuid())).isEqualTo(2);
    } finally {
      deleteAllInfraDefs(infraDef1, infraDef2);
    }
  }

  private void deleteAllInfraDefs(InfrastructureDefinition... infrastructureDefinitions) {
    for (InfrastructureDefinition infrastructureDefinition : infrastructureDefinitions) {
      wingsPersistence.delete(InfrastructureDefinition.class, infrastructureDefinition.getUuid());
    }
  }

  private void deleteEnvironment(Environment env) {
    environmentService.delete(env.getAppId(), env.getUuid());
  }

  private Environment createEnvironment(String name) {
    Environment env = Environment.Builder.anEnvironment().appId(app1.getUuid()).name(name).build();
    return environmentService.save(env);
  }

  private void shouldLimitWhenMoreEntries(Environment env) {
    InfrastructureDefinition infraDef1 = InfrastructureDefinition.builder()
                                             .appId(env.getAppId())
                                             .envId(env.getUuid())
                                             .name("infra1")
                                             .infrastructure(PhysicalInfra.builder().build())
                                             .build();
    InfrastructureDefinition infraDef2 = InfrastructureDefinition.builder()
                                             .appId(env.getAppId())
                                             .envId(env.getUuid())
                                             .name("infra2")
                                             .infrastructure(PhysicalInfra.builder().build())
                                             .build();
    saveAll(infraDef1, infraDef2);

    try {
      List<InfrastructureDefinition> result =
          infrastructureDefinitionService.getNameAndIdForEnvironment(env.getAppId(), env.getUuid(), 1);

      assertThat(result).hasSize(1);
    } finally {
      deleteAllInfraDefs(infraDef1, infraDef2);
    }
  }

  private void shouldGetWhenNoEntries(Environment env) {
    assertThat(infrastructureDefinitionService.getNameAndIdForEnvironment(env.getAppId(), env.getUuid(), 1)).isEmpty();
  }

  @After
  public void cleanUp() {
    appService.delete(app1.getUuid());
    appService.delete(app2.getUuid());
  }

  private void setRequiredFields(InfrastructureDefinition infrastructureDefinition) {
    assert infrastructureDefinition != null;
    if (EmptyPredicate.isEmpty(infrastructureDefinition.getEnvId())) {
      if (app1.getUuid().equals(infrastructureDefinition.getAppId())) {
        infrastructureDefinition.setEnvId(app1_env1.getUuid());
      } else if (app2.getUuid().equals(infrastructureDefinition.getAppId())) {
        infrastructureDefinition.setEnvId(app2_env2.getUuid());
      }
    }
    if (infrastructureDefinition.getInfrastructure() == null) {
      infrastructureDefinition.setInfrastructure(
          AwsInstanceInfrastructure.builder().cloudProviderId(cloudProvider.getUuid()).build());
    }
    if (infrastructureDefinition.getCloudProviderType() == null) {
      infrastructureDefinition.setCloudProviderType(CloudProviderType.AWS);
    }
    if (infrastructureDefinition.getDeploymentType() == null) {
      infrastructureDefinition.setDeploymentType(DeploymentType.SSH);
    }
  }
}
