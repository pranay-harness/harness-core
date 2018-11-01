package software.wings.yaml.handler.inframappings;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.Key;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.PcfInfrastructureMapping.Yaml;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.inframapping.PcfInfraMappingYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.handler.BaseYamlHandlerTest;

import java.io.IOException;
import java.util.Arrays;

public class PcfInfraMappingYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock private SettingsService settingsService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private YamlChangeSetService yamlChangeSetService;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Mock @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  private String validYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: PCF_PCF\n"
      + "computeProviderName: COMPUTE_PROVIDER_NAME\n"
      + "computeProviderType: PCF\n"
      + "deploymentType: PCF\n"
      + "infraMappingType: PCF_PCF\n"
      + "organization: ORG\n"
      + "routeMaps:\n"
      + "- ROUTE\n"
      + "serviceName: SERVICE_NAME\n"
      + "space: SPCAE\n"
      + "tempRouteMap:\n"
      + "- ROUTE";

  private String invalidYamlContent = "harnessApiVersion: '1.0'\n"
      + "type: PCFV\n"
      + "computeProviderName: COMPUTE_PROVIDER_NAME\n"
      + "computeProviderType: PCF\n"
      + "deploymentType1: PCF\n"
      + "infraMappingType1: PCF\n"
      + "organization1: ORG\n"
      + "routeMaps:\n"
      + "- ROUTE\n"
      + "serviceName: SERVICE_NAME\n"
      + "space: SPCAE\n"
      + "tempRouteMap:\n"
      + "- ROUTE";

  @InjectMocks @Inject private PcfInfraMappingYamlHandler yamlHandler;
  @InjectMocks @Inject private InfrastructureMappingService infrastructureMappingService;
  @InjectMocks @Inject protected YamlHelper yamlHelper;

  private String validYamlFilePath =
      "Setup/Applications/APP_NAME/Environments/ENV_NAME/Service Infrastructure/pcf.yaml";
  private String infraMappingName = "pcf";
  private static String ROUTE = "ROUTE";
  private static String SPACE = "SPCAE";
  private static String ORG = "ORG";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(settingsService.getByName(anyString(), anyString(), anyString())).thenReturn(getSettingAttribute());
    when(settingsService.get(anyString())).thenReturn(getSettingAttribute());
    when(appService.get(anyString())).thenReturn(getApplication());
    when(appService.getAppByName(anyString(), anyString())).thenReturn(getApplication());
    when(serviceResourceService.get(anyString(), anyString())).thenReturn(getService());
    when(serviceResourceService.getServiceByName(anyString(), anyString())).thenReturn(getService());
    when(environmentService.getEnvironmentByName(anyString(), anyString()))
        .thenReturn(Environment.Builder.anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    when(serviceTemplateService.getTemplateRefKeysByService(anyString(), anyString(), anyString()))
        .thenReturn(Arrays.asList(new Key(ServiceTemplate.class, "serviceTemplates", SERVICE_ID)));
    when(serviceTemplateService.get(anyString(), anyString()))
        .thenReturn(ServiceTemplate.Builder.aServiceTemplate().withUuid("uuid").withName("name").build());
    when(yamlChangeSetService.saveChangeSet(any(), any(), any())).thenReturn(null);
    when(yamlChangeSetService.saveChangeSet(any(), any(), any())).thenReturn(null);
    when(yamlDirectoryService.weNeedToPushChanges(any(), any())).thenReturn(null);
  }

  private Service getService() {
    return Service.builder().name(SERVICE_NAME).uuid(SERVICE_ID).build();
  }

  private Application getApplication() {
    return Application.Builder.anApplication().withName("app").withUuid("id").build();
  }

  @Test
  public void testCRUDAndGet() throws HarnessException, IOException {
    ChangeContext<PcfInfrastructureMapping.Yaml> changeContext =
        getChangeContext(validYamlContent, validYamlFilePath, yamlHandler);

    Yaml yamlObject = (Yaml) getYaml(validYamlContent, Yaml.class);
    changeContext.setYaml(yamlObject);

    PcfInfrastructureMapping ecsInfraMapping = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertNotNull(ecsInfraMapping);
    assertEquals(ecsInfraMapping.getName(), infraMappingName);

    Yaml yaml = yamlHandler.toYaml(ecsInfraMapping, APP_ID);
    assertNotNull(yaml);
    assertEquals(InfrastructureMappingType.PCF_PCF.name(), yaml.getType());

    String yamlContent = getYamlContent(yaml);
    assertNotNull(yamlContent);
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertEquals(validYamlContent, yamlContent);

    InfrastructureMapping infraMapping = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNotNull(infraMapping);
    assertEquals(infraMapping.getName(), infraMappingName);

    yamlHandler.delete(changeContext);

    InfrastructureMapping deletedInfraMapping = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertNull(deletedInfraMapping);
  }

  @Test
  public void testFailures() throws HarnessException, IOException {
    ChangeContext<PcfInfrastructureMapping.Yaml> changeContext =
        getChangeContext(invalidYamlContent, validYamlFilePath, yamlHandler);

    PcfInfrastructureMapping.Yaml yamlObject =
        (PcfInfrastructureMapping.Yaml) getYaml(validYamlContent, PcfInfrastructureMapping.Yaml.class);
    changeContext.setYaml(yamlObject);

    yamlObject = (PcfInfrastructureMapping.Yaml) getYaml(invalidYamlContent, PcfInfrastructureMapping.Yaml.class);
    changeContext.setYaml(yamlObject);
    thrown.expect(Exception.class);
    yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
  }
  private SettingAttribute getSettingAttribute() {
    return aSettingAttribute()
        .withName("COMPUTE_PROVIDER_NAME")
        .withUuid(SETTING_ID)
        .withValue(PcfConfig.builder().username(USER_NAME).password(PASSWORD).endpointUrl("url").build())
        .build();
  }

  protected <Y extends BaseYaml, H extends BaseYamlHandler> ChangeContext<Y> getChangeContext(
      String yamlContent, String yamlFilePath, H yamlHandler) {
    // Invalid yaml path
    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withAccountId(ACCOUNT_ID)
                                      .withFilePath(yamlFilePath)
                                      .withFileContent(yamlContent)
                                      .build();

    ChangeContext<Y> changeContext = new ChangeContext();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.INFRA_MAPPING);
    changeContext.setYamlSyncHandler(yamlHandler);
    return changeContext;
  }
}
