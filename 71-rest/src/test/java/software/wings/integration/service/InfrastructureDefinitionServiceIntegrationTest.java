package software.wings.integration.service;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
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
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InfrastructureDefinitionServiceIntegrationTest extends BaseIntegrationTest {
  @InjectMocks @Inject private AppService appService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private EnvironmentService environmentService;

  private Application app1, app2;
  private Environment app1_env1, app2_env2;
  private SettingAttribute cloudProvider;
  private Set<String> allInfraDefinitionsCreated = new HashSet<>();

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
    Assertions.assertThat(infraDefNames).containsExactlyInAnyOrder("id1", "id2");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(DeprecatedIntegrationTests.class)
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
    Assertions.assertThat(infraDefNames).containsExactlyInAnyOrder("id1", "id3");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(DeprecatedIntegrationTests.class)
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
    Assertions.assertThat(infraDefNames).containsExactlyInAnyOrder("id1", "id3");
  }

  private void saveAll(InfrastructureDefinition... defs) {
    for (InfrastructureDefinition infrastructureDefinition : defs) {
      setRequiredFields(infrastructureDefinition);
      wingsPersistence.save(infrastructureDefinition);
      allInfraDefinitionsCreated.add(infrastructureDefinition.getUuid());
    }
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
