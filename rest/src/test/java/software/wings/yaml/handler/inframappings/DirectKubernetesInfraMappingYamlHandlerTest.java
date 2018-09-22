package software.wings.yaml.handler.inframappings;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.Key;
import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.HarnessException;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.DirectKubernetesInfraMappingYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.utils.ArtifactType;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;

public class DirectKubernetesInfraMappingYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock protected SettingsService settingsService;
  @Mock protected ServiceResourceService serviceResourceService;
  @Mock protected ServiceTemplateService serviceTemplateService;
  @Mock protected AppService appService;
  @Mock protected EnvironmentService environmentService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private ContainerService containerService;
  @Mock @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Mock private SecretManager secretManager;

  @InjectMocks @Inject protected YamlHelper yamlHelper;
  @InjectMocks @Inject protected InfrastructureMappingService infrastructureMappingService;
  @InjectMocks @Inject protected DirectKubernetesInfraMappingYamlHandler directKubernetesInfraMappingYamlHandler;

  private String validYamlContentWithKubernetesClusterCloudProvider = "harnessApiVersion: '1.0'\n"
      + "type: DIRECT_KUBERNETES\n"
      + "computeProviderName: kubernetes_cluster\n"
      + "computeProviderType: KUBERNETES_CLUSTER\n"
      + "deploymentType: KUBERNETES\n"
      + "infraMappingType: DIRECT_KUBERNETES\n"
      + "namespace: default\n"
      + "serviceName: dockersvc";

  private String invalidYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: DIRECT_KUBERNETES\n"
      + "serviceName: dockersvc";

  private String validYamlFilePath =
      "Setup/Applications/APP_NAME/Environments/ENVIRONMENT_NAME/Service Infrastructure/direct_kubernetes.yaml";
  private String infraMappingName = "direct_kubernetes";
  private String serviceName = "dockersvc";
  private String computeProviderName = "kubernetes_cluster";
  private ServiceTemplate serviceTemplate =
      ServiceTemplate.Builder.aServiceTemplate().withUuid("uuid").withName("name").build();
  private SettingAttribute settingAttribute = getSettingAttribute();

  @InjectMocks @Inject private DirectKubernetesInfraMappingYamlHandler yamlHandler;
  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(settingsService.getByName(anyString(), anyString(), anyString())).thenReturn(settingAttribute);
    when(settingsService.get(anyString())).thenReturn(settingAttribute);
    when(appService.get(anyString())).thenReturn(getApplication());
    when(appService.getAppByName(anyString(), anyString())).thenReturn(getApplication());
    when(environmentService.getEnvironmentByName(anyString(), anyString())).thenReturn(getEnvironment());
    when(containerService.validate(anyObject())).thenReturn(true);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(containerService);
    when(serviceResourceService.getServiceByName(anyString(), anyString())).thenReturn(getService());
    when(serviceResourceService.get(anyString(), anyString())).thenReturn(getService());
    when(serviceTemplateService.getTemplateRefKeysByService(anyString(), anyString(), anyString()))
        .thenReturn(asList(new Key<ServiceTemplate>(ServiceTemplate.class, "serviceTemplates", SERVICE_ID)));
    when(serviceTemplateService.get(anyString(), anyString())).thenReturn(serviceTemplate);
  }

  private Service getService() {
    return Service.builder().name(serviceName).appId(APP_ID).uuid(SERVICE_ID).artifactType(ArtifactType.DOCKER).build();
  }

  private Environment getEnvironment() {
    return Environment.Builder.anEnvironment().withUuid("ANY_UUID").withName("ENV_NAME").build();
  }

  private Application getApplication() {
    return Application.Builder.anApplication()
        .withUuid("ANY_UUID")
        .withName(APP_NAME)
        .withAccountId(ACCOUNT_ID)
        .build();
  }

  private SettingAttribute getSettingAttribute() {
    return aSettingAttribute()
        .withName(computeProviderName)
        .withUuid(SETTING_ID)
        .withValue(AzureConfig.builder().clientId("ClientId").tenantId("tenantId").key("key".toCharArray()).build())
        .build();
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    ChangeContext<DirectKubernetesInfrastructureMapping.Yaml> changeContext =
        getChangeContext(validYamlContentWithKubernetesClusterCloudProvider, validYamlFilePath, yamlHandler);

    DirectKubernetesInfrastructureMapping.Yaml yamlObject = (DirectKubernetesInfrastructureMapping.Yaml) getYaml(
        validYamlContentWithKubernetesClusterCloudProvider, DirectKubernetesInfrastructureMapping.Yaml.class);
    changeContext.setYaml(yamlObject);

    DirectKubernetesInfrastructureMapping infraMapping =
        yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertNotNull(infraMapping);
    assertEquals(infraMapping.getName(), infraMappingName);

    DirectKubernetesInfrastructureMapping.Yaml yaml = yamlHandler.toYaml(infraMapping, APP_ID);
    assertNotNull(yaml);
    assertEquals("DIRECT_KUBERNETES", yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(validYamlContentWithKubernetesClusterCloudProvider, yamlContent);

    InfrastructureMapping infraMapping1 = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNotNull(infraMapping1);
    assertEquals(infraMapping1.getName(), infraMappingName);

    yamlHandler.delete(changeContext);

    InfrastructureMapping deletedInfraMapping = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNull(deletedInfraMapping);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    ChangeContext<DirectKubernetesInfrastructureMapping.Yaml> changeContext =
        getChangeContext(invalidYamlContent, validYamlFilePath, yamlHandler);

    DirectKubernetesInfrastructureMapping.Yaml yamlObject = (DirectKubernetesInfrastructureMapping.Yaml) getYaml(
        validYamlContentWithKubernetesClusterCloudProvider, DirectKubernetesInfrastructureMapping.Yaml.class);
    changeContext.setYaml(yamlObject);

    yamlObject = (DirectKubernetesInfrastructureMapping.Yaml) getYaml(
        invalidYamlContent, DirectKubernetesInfrastructureMapping.Yaml.class);
    changeContext.setYaml(yamlObject);
    thrown.expect(Exception.class);
    yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
  }

  protected <Y extends BaseYaml, H extends BaseYamlHandler> ChangeContext<Y> getChangeContext(
      String yamlContent, String yamlFilePath, H yamlHandler) {
    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withAccountId(ACCOUNT_ID)
                                      .withFilePath(yamlFilePath)
                                      .withFileContent(yamlContent)
                                      .build();

    ChangeContext<Y> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.INFRA_MAPPING);
    changeContext.setYamlSyncHandler(yamlHandler);
    return changeContext;
  }
}
