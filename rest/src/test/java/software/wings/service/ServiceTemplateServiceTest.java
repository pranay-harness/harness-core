package software.wings.service;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.Builder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.infrastructure.ApplicationHost.Builder.anApplicationHost;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ServiceTemplateServiceImpl;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 4/29/16.
 */
public class ServiceTemplateServiceTest extends WingsBaseTest {
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
  @Mock private InfrastructureService infrastructureService;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private EnvironmentService environmentService;
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
    when(serviceResourceService.get(APP_ID, SERVICE_ID))
        .thenReturn(aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build());
    when(wingsPersistence.createQuery(ServiceTemplate.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
    when(end.hasThisElement(any())).thenReturn(query);
  }

  /**
   * Should list saved service templates.
   */
  @Test
  public void shouldListSavedServiceTemplates() {
    PageResponse<ServiceTemplate> pageResponse = new PageResponse<>();
    ApplicationHost host = ApplicationHost.Builder.anApplicationHost().withUuid(HOST_ID).build();

    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(builder.but().withServiceId(SERVICE_ID).build()));

    when(wingsPersistence.query(ServiceTemplate.class, pageRequest)).thenReturn(pageResponse);
    when(hostService.list(any(PageRequest.class))).thenReturn(aPageResponse().withResponse(asList(host)).build());

    PageResponse<ServiceTemplate> templatePageResponse = templateService.list(pageRequest, true);

    ServiceTemplate expectedServiceTemplate = builder.but().withServiceId(SERVICE_ID).build();
    expectedServiceTemplate.setService(
        aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build());

    assertThat(templatePageResponse).isInstanceOf(PageResponse.class);
    assertThat(pageResponse.getResponse().get(0)).isEqualTo(expectedServiceTemplate);
  }

  /**
   * Should save service template.
   */
  @Test
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
  public void shouldCreateDefaultServiceTemplateByEnv() {
    Service service = aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
    when(serviceResourceService.findServicesByApp(APP_ID)).thenReturn(asList(service));
    templateService.createDefaultTemplatesByEnv(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build());
    verify(serviceResourceService).findServicesByApp(APP_ID);
    verify(wingsPersistence)
        .saveAndGet(ServiceTemplate.class,
            aServiceTemplate()
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
  public void shouldCreateDefaultServiceTemplateByService() {
    Service service = aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
    Environment environment = Builder.anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build();
    when(environmentService.getEnvByApp(APP_ID)).thenReturn(asList(environment));
    templateService.createDefaultTemplatesByService(service);
    verify(environmentService).getEnvByApp(APP_ID);
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
  public void shouldUpdateServiceTemplate() {
    ServiceTemplate template = builder.build();
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);
    templateService.update(template);
    verify(wingsPersistence)
        .updateFields(
            ServiceTemplate.class, TEMPLATE_ID, of("name", TEMPLATE_NAME, "description", TEMPLATE_DESCRIPTION));
  }

  /**
   * Should delete service template.
   */
  @Test
  public void shouldDeleteServiceTemplate() {
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    templateService.delete(APP_ID, ENV_ID, TEMPLATE_ID);
    verify(wingsPersistence).delete(query);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(TEMPLATE_ID);
    verify(serviceInstanceService).deleteByServiceTemplate(APP_ID, ENV_ID, TEMPLATE_ID);
  }

  /**
   * Should delete by env.
   */
  @Test
  public void shouldDeleteByEnv() {
    when(query.asList())
        .thenReturn(asList(aServiceTemplate()
                               .withUuid(TEMPLATE_ID)
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withName(TEMPLATE_NAME)
                               .build()));
    doNothing().when(spyTemplateService).delete(APP_ID, ENV_ID, TEMPLATE_ID);
    spyTemplateService.deleteByEnv(APP_ID, ENV_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
  }

  /**
   * Should delete by service.
   */
  @Test
  public void shouldDeleteByService() {
    doNothing().when(spyTemplateService).delete(APP_ID, ENV_ID, TEMPLATE_ID);
    when(query.asList())
        .thenReturn(asList(aServiceTemplate()
                               .withUuid(TEMPLATE_ID)
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withName(TEMPLATE_NAME)
                               .build()));
    spyTemplateService.deleteByService(APP_ID, SERVICE_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("serviceId");
    verify(end).equal(SERVICE_ID);
    verify(spyTemplateService).delete(APP_ID, ENV_ID, TEMPLATE_ID);
  }

  /**
   * Should override config files.
   */
  @Test
  public void shouldOverrideConfigFiles() {
    List<ConfigFile> existingFiles = asList(aConfigFile().withUuid("FILE_ID_1").withName("app.properties").build(),
        aConfigFile().withUuid("FILE_ID_2").withName("cache.xml").build());

    List<ConfigFile> newFiles = asList(aConfigFile().withUuid("FILE_ID_3").withName("app.properties").build(),
        aConfigFile().withUuid("FILE_ID_4").withName("cache.xml").build());

    List<ConfigFile> computedConfigFiles = templateService.overrideConfigFiles(existingFiles, newFiles);
    assertThat(computedConfigFiles).isEqualTo(newFiles);
  }

  @Test
  @Ignore
  public void shouldComputeConfigFilesForHosts() {
    when(wingsPersistence.get(ServiceTemplate.class, TEMPLATE_ID)).thenReturn(builder.build());

    when(configService.getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_1").withName("PROPERTIES_FILE").build()));

    when(configService.getConfigFilesForEntity(APP_ID, "TEMPLATE_ID", "HOST_ID_1"))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_3").withName("PROPERTIES_FILE").build()));

    Map<String, List<ConfigFile>> hostConfigFileMapping =
        templateService.computedConfigFiles(APP_ID, ENV_ID, TEMPLATE_ID);
    assertThat(hostConfigFileMapping.get("HOST_NAME_1"))
        .isEqualTo(asList(aConfigFile().withUuid("FILE_ID_3").withName("PROPERTIES_FILE").build()));
  }
}
