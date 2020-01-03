package software.wings.service;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.pcf.model.PcfConstants.VARS_YML;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.RAGHU;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.appmanifest.ManifestFile.VALUES_YAML_KEY;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ServiceTemplateServiceImpl;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.utils.ArtifactType;

import java.util.Arrays;
import java.util.List;

/**
 * Created by anubhaw on 4/29/16.
 */
public class ServiceTemplateServiceTest extends WingsBaseTest {
  private final String APP_MANIFEST_ID = "APP_MANIFEST_ID";

  /**
   * The Query.
   */
  @Mock Query<ServiceTemplate> query;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ConfigService configService;
  @Mock private HostService hostService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private EnvironmentService environmentService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ServiceVariableService serviceVariableService;
  @Mock private ApplicationManifestService appManifestService;

  @Inject @InjectMocks private ServiceTemplateService templateService;
  @Spy @InjectMocks private ServiceTemplateService spyTemplateService = new ServiceTemplateServiceImpl();
  private ServiceTemplate.Builder builder = aServiceTemplate()
                                                .withUuid(TEMPLATE_ID)
                                                .withAppId(APP_ID)
                                                .withEnvId(ENV_ID)
                                                .withServiceId(SERVICE_ID)
                                                .withName(TEMPLATE_NAME)
                                                .withDescription(TEMPLATE_DESCRIPTION);

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID))
        .thenReturn(Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(wingsPersistence.createQuery(ServiceTemplate.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
  }

  /**
   * Should list saved service templates.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldListSavedServiceTemplates() {
    PageResponse<ServiceTemplate> pageResponse =
        aPageResponse().withResponse(asList(builder.but().withServiceId(SERVICE_ID).build())).build();

    when(wingsPersistence.query(ServiceTemplate.class, aPageRequest().build())).thenReturn(pageResponse);
    when(infrastructureMappingService.list(any(PageRequest.class))).thenReturn(aPageResponse().build());

    PageResponse<ServiceTemplate> templatePageResponse =
        templateService.list(aPageRequest().build(), true, OBTAIN_VALUE);

    ServiceTemplate expectedServiceTemplate = builder.but().withServiceId(SERVICE_ID).build();

    assertThat(templatePageResponse).isInstanceOf(PageResponse.class);
    assertThat(pageResponse.getResponse().get(0)).isEqualTo(expectedServiceTemplate);
  }

  /**
   * Should save service template.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldSaveServiceTemplate() {
    when(wingsPersistence.saveAndGet(eq(ServiceTemplate.class), any(ServiceTemplate.class)))
        .thenReturn(builder.build());
    ServiceTemplate template = templateService.save(builder.build());
    assertThat(template.getName()).isEqualTo(TEMPLATE_NAME);
    assertThat(template.getServiceId()).isEqualTo(SERVICE_ID);
  }

  /**
   * Should create default service template by env.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldCreateDefaultServiceTemplateByEnv() {
    Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
    when(serviceResourceService.findServicesByAppInternal(APP_ID)).thenReturn(asList(service));
    templateService.createDefaultTemplatesByEnv(anEnvironment().appId(APP_ID).uuid(ENV_ID).build());
    verify(serviceResourceService).findServicesByAppInternal(APP_ID);
    verify(wingsPersistence)
        .saveAndGet(ServiceTemplate.class,
            aServiceTemplate()
                .withDefaultServiceTemplate(true)
                .withAppId(APP_ID)
                .withEnvId(ENV_ID)
                .withServiceId(service.getUuid())
                .withName(service.getName())
                .build());
  }

  /**
   * Should create default service template by service.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldCreateDefaultServiceTemplateByService() {
    Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
    Environment environment = Builder.anEnvironment().appId(APP_ID).uuid(ENV_ID).build();
    when(environmentService.getEnvIdsByApp(APP_ID)).thenReturn(asList(environment.getUuid()));
    templateService.createDefaultTemplatesByService(service);
    verify(environmentService).getEnvIdsByApp(APP_ID);
    verify(wingsPersistence)
        .saveAndGet(ServiceTemplate.class,
            aServiceTemplate()
                .withAppId(APP_ID)
                .withEnvId(ENV_ID)
                .withServiceId(service.getUuid())
                .withName(service.getName())
                .withDefaultServiceTemplate(true)
                .build());
  }

  /**
   * Should update service template.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldUpdateServiceTemplate() {
    ServiceTemplate template = builder.build();
    when(wingsPersistence.getWithAppId(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);
    when(infrastructureMappingService.list(any(PageRequest.class))).thenReturn(aPageResponse().build());
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(anEnvironment().build());
    templateService.update(template);
    verify(wingsPersistence)
        .updateFields(
            ServiceTemplate.class, TEMPLATE_ID, of("name", TEMPLATE_NAME, "description", TEMPLATE_DESCRIPTION));
  }

  /**
   * Should delete service template.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldDeleteServiceTemplate() {
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    templateService.delete(APP_ID, TEMPLATE_ID);
    verify(wingsPersistence).delete(query);
    verify(query).filter("appId", APP_ID);
    verify(query).filter(ID_KEY, TEMPLATE_ID);
    verify(infrastructureMappingService).deleteByServiceTemplate(APP_ID, TEMPLATE_ID);
    verify(configService).deleteByTemplateId(APP_ID, TEMPLATE_ID);
    verify(serviceVariableService).deleteByTemplateId(APP_ID, TEMPLATE_ID);
  }

  /**
   * Should prune by environment.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldPruneByEnvironment() {
    when(query.asList())
        .thenReturn(asList(aServiceTemplate()
                               .withUuid(TEMPLATE_ID)
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withName(TEMPLATE_NAME)
                               .build()));
    doNothing().when(spyTemplateService).delete(APP_ID, TEMPLATE_ID);
    spyTemplateService.pruneByEnvironment(APP_ID, ENV_ID);
    verify(query).filter("appId", APP_ID);
    verify(query).filter("envId", ENV_ID);
  }

  /**
   * Should delete by service.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldPruneByService() {
    doNothing().when(spyTemplateService).delete(APP_ID, TEMPLATE_ID);
    when(query.asList())
        .thenReturn(asList(aServiceTemplate()
                               .withUuid(TEMPLATE_ID)
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withName(TEMPLATE_NAME)
                               .build()));
    spyTemplateService.pruneByService(APP_ID, SERVICE_ID);
    verify(query).filter("appId", APP_ID);
    verify(query).filter("serviceId", SERVICE_ID);
    verify(spyTemplateService).delete(APP_ID, TEMPLATE_ID);
  }

  /**
   * Should override config files.
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldOverrideConfigFiles() {
    ConfigFile configFile1 = ConfigFile.builder().relativeFilePath("app.properties").build();
    configFile1.setName("app.properties");
    configFile1.setUuid("FILE_ID_1");

    ConfigFile configFile2 = ConfigFile.builder().relativeFilePath("cache.xml").build();
    configFile2.setName("cache.xml");
    configFile2.setUuid("FILE_ID_2");

    ConfigFile configFile3 = ConfigFile.builder().relativeFilePath("app.properties").build();
    configFile3.setName("app.properties");
    configFile3.setUuid("FILE_ID_3");

    ConfigFile configFile4 = ConfigFile.builder().relativeFilePath("cache.xml").build();
    configFile4.setName("cache.xml");
    configFile4.setUuid("FILE_ID_4");
    List<ConfigFile> existingFiles = asList(configFile1, configFile2);

    List<ConfigFile> newFiles = asList(configFile3, configFile4);

    List<ConfigFile> computedConfigFiles = templateService.overrideConfigFiles(existingFiles, newFiles);
    assertThat(computedConfigFiles).isEqualTo(newFiles);
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldComputeConfigFilesForHosts() {
    when(wingsPersistence.get(ServiceTemplate.class, TEMPLATE_ID)).thenReturn(builder.build());

    ConfigFile configFile1 = ConfigFile.builder().build();
    configFile1.setName("PROPERTIES_FILE");
    configFile1.setUuid("FILE_ID_1");
    when(configService.getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID))
        .thenReturn(asList(configFile1));

    ConfigFile configFile2 = ConfigFile.builder().build();
    configFile1.setName("PROPERTIES_FILE");
    configFile1.setUuid("FILE_ID_3");
    when(configService.getConfigFilesForEntity(APP_ID, "TEMPLATE_ID", "HOST_ID_1")).thenReturn(asList(configFile2));

    List<ConfigFile> hostConfigFiles = templateService.computedConfigFiles(APP_ID, ENV_ID, TEMPLATE_ID);
    assertThat(hostConfigFiles).isEqualTo(asList(configFile2));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testPopulateServiceAndOverrideValuesManifestFile() {
    PageResponse<ServiceTemplate> pageResponse =
        aPageResponse().withResponse(asList(builder.but().withServiceId(SERVICE_ID).build())).build();

    when(wingsPersistence.query(ServiceTemplate.class, aPageRequest().build())).thenReturn(pageResponse);
    when(infrastructureMappingService.list(any(PageRequest.class))).thenReturn(aPageResponse().build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID))
        .thenReturn(Service.builder().artifactType(ArtifactType.PCF).build());

    PageResponse<ServiceTemplate> templatePageResponse =
        templateService.list(aPageRequest().build(), true, OBTAIN_VALUE);
    assertThat(templatePageResponse).isInstanceOf(PageResponse.class);
    ServiceTemplate serviceTemplate = pageResponse.getResponse().get(0);
    assertThat(serviceTemplate.getValuesOverrideManifestFile()).isNull();
    verify(appManifestService, times(0)).getManifestFilesByAppManifestId(any(), any());

    ApplicationManifest appManifest =
        ApplicationManifest.builder().storeType(StoreType.Local).kind(AppManifestKind.PCF_OVERRIDE).build();
    appManifest.setAppId(APP_ID);
    appManifest.setUuid(APP_MANIFEST_ID);
    when(appManifestService.getAppManifest(APP_ID, ENV_ID, SERVICE_ID, AppManifestKind.PCF_OVERRIDE))
        .thenReturn(appManifest);

    templatePageResponse = templateService.list(aPageRequest().build(), true, OBTAIN_VALUE);
    assertThat(templatePageResponse).isInstanceOf(PageResponse.class);
    serviceTemplate = pageResponse.getResponse().get(0);
    assertThat(serviceTemplate.getValuesOverrideManifestFile()).isNull();
    verify(appManifestService, times(1)).getManifestFilesByAppManifestId(any(), any());

    when(appManifestService.getManifestFilesByAppManifestId(APP_ID, APP_MANIFEST_ID))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileName(VARS_YML).fileContent("vars").build()));

    templatePageResponse = templateService.list(aPageRequest().build(), true, OBTAIN_VALUE);
    assertThat(templatePageResponse).isInstanceOf(PageResponse.class);
    serviceTemplate = pageResponse.getResponse().get(0);
    assertThat(serviceTemplate.getValuesOverrideManifestFile().getFileName()).isEqualTo(VARS_YML);
    assertThat(serviceTemplate.getValuesOverrideManifestFile().getFileContent()).isEqualTo("vars");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testPopulateServiceAndOverrideValuesManifestFileForValues() {
    PageResponse<ServiceTemplate> pageResponse =
        aPageResponse().withResponse(asList(builder.but().withServiceId(SERVICE_ID).build())).build();

    when(wingsPersistence.query(ServiceTemplate.class, aPageRequest().build())).thenReturn(pageResponse);
    when(infrastructureMappingService.list(any(PageRequest.class))).thenReturn(aPageResponse().build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(Service.builder().build());

    ApplicationManifest appManifest =
        ApplicationManifest.builder().storeType(StoreType.Local).kind(AppManifestKind.VALUES).build();
    appManifest.setAppId(APP_ID);
    appManifest.setUuid(APP_MANIFEST_ID);
    when(appManifestService.getAppManifest(APP_ID, ENV_ID, SERVICE_ID, AppManifestKind.VALUES)).thenReturn(appManifest);
    when(appManifestService.getManifestFilesByAppManifestId(APP_ID, APP_MANIFEST_ID))
        .thenReturn(Arrays.asList(ManifestFile.builder().fileName(VALUES_YAML_KEY).fileContent("values").build()));

    PageResponse<ServiceTemplate> templatePageResponse =
        templateService.list(aPageRequest().build(), true, OBTAIN_VALUE);
    assertThat(templatePageResponse).isInstanceOf(PageResponse.class);
    ServiceTemplate serviceTemplate = pageResponse.getResponse().get(0);
    assertThat(serviceTemplate.getValuesOverrideManifestFile().getFileName()).isEqualTo(VALUES_YAML_KEY);
    assertThat(serviceTemplate.getValuesOverrideManifestFile().getFileContent()).isEqualTo("values");
  }
}
