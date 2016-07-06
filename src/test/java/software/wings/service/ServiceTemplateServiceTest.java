package software.wings.service;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.ConfigFileBuilder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.Tag.Builder.aTag;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.INFRA_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TAG_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Host;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ServiceTemplateServiceImpl;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

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
  @Mock private TagService tagService;
  @Mock private HostService hostService;
  @Mock private InfraService infraService;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private EnvironmentService environmentService;
  @Inject @InjectMocks private ServiceTemplateService templateService;
  @Spy @InjectMocks private ServiceTemplateService spyTemplateService = new ServiceTemplateServiceImpl();
  private ServiceTemplate.Builder builder = aServiceTemplate()
                                                .withUuid(TEMPLATE_ID)
                                                .withAppId(APP_ID)
                                                .withEnvId(ENV_ID)
                                                .withService(aService().withUuid(SERVICE_ID).build())
                                                .withName(TEMPLATE_NAME)
                                                .withDescription(TEMPLATE_DESCRIPTION);

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
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
    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(builder.build()));
    when(wingsPersistence.query(ServiceTemplate.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ServiceTemplate> templates = templateService.list(pageRequest);
    assertThat(templates).containsAll(asList(builder.build()));
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
    assertThat(template.getService().getUuid()).isEqualTo(SERVICE_ID);
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
                .withService(service)
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
                .withService(service)
                .withName(service.getName())
                .build());
  }

  /**
   * Should update service template.
   */
  @Test
  public void shouldUpdateServiceTemplate() {
    ServiceTemplate template = builder.build();
    templateService.update(template);
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID,
            of("name", TEMPLATE_NAME, "description", TEMPLATE_DESCRIPTION, "service",
                aService().withUuid(SERVICE_ID).build()));
  }

  /**
   * Should delete service template.
   */
  @Test
  public void shouldDeleteServiceTemplate() {
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
    verify(query).field("service");
    verify(end).equal(SERVICE_ID);
    verify(spyTemplateService).delete(APP_ID, ENV_ID, TEMPLATE_ID);
  }

  /**
   * Should add hosts.
   */
  @Test
  public void shouldAddHosts() {
    Host host = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID").build();
    when(hostService.get(APP_ID, INFRA_ID, HOST_ID)).thenReturn(host);
    when(infraService.getInfraIdByEnvId(APP_ID, ENV_ID)).thenReturn(INFRA_ID);
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(builder.build());

    ServiceTemplate template = builder.build();
    templateService.updateHosts(APP_ID, ENV_ID, template.getUuid(), asList(HOST_ID));
    verify(wingsPersistence).updateFields(ServiceTemplate.class, template.getUuid(), of("hosts", asList(host)));
    verify(serviceInstanceService).updateInstanceMappings(template, asList(host), asList());
  }

  /**
   * Should delete hosts.
   */
  @Test
  public void shouldDeleteHosts() {
    Host host = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID").build();
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID))
        .thenReturn(builder.withHosts(asList(host)).build());

    ServiceTemplate template = builder.build();
    templateService.updateHosts(APP_ID, ENV_ID, template.getUuid(), asList());
    verify(wingsPersistence).updateFields(ServiceTemplate.class, template.getUuid(), of("hosts", asList()));
    verify(serviceInstanceService).updateInstanceMappings(template, asList(), asList(host));
  }

  /**
   * Should add and delete hosts.
   */
  @Test
  public void shouldAddAndDeleteHosts() {
    Host existingHost = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID_1").build();
    Host newHost = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID_2").build();
    when(hostService.get(APP_ID, INFRA_ID, "HOST_ID_2")).thenReturn(newHost);
    when(infraService.getInfraIdByEnvId(APP_ID, ENV_ID)).thenReturn(INFRA_ID);
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID))
        .thenReturn(builder.withHosts(asList(existingHost)).build());

    ServiceTemplate template = builder.build();
    templateService.updateHosts(APP_ID, ENV_ID, template.getUuid(), asList("HOST_ID_2"));
    verify(wingsPersistence).updateFields(ServiceTemplate.class, template.getUuid(), of("hosts", asList(newHost)));
    verify(serviceInstanceService).updateInstanceMappings(template, asList(newHost), asList(existingHost));
  }

  /**
   * Should add tags.
   */
  @Test
  public void shouldAddTags() {
    Tag tag = aTag().withEnvId(ENV_ID).withUuid(TAG_ID).build();
    Host host = aHost().withUuid(HOST_ID).build();
    ServiceTemplate template = builder.withUuid(TEMPLATE_ID).build();
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);
    when(tagService.get(APP_ID, ENV_ID, TAG_ID)).thenReturn(tag);
    when(tagService.getLeafTags(tag)).thenReturn(asList(tag));
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(tag))).thenReturn(asList(host));

    templateService.updateTags(APP_ID, ENV_ID, TEMPLATE_ID, asList(TAG_ID));

    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID, of("tags", asList(tag), "leafTags", singleton(tag)));
    verify(serviceInstanceService).updateInstanceMappings(template, asList(host), asList());
  }

  /**
   * Should delete tags.
   */
  @Test
  public void shouldDeleteTags() {
    Tag tag = aTag().withEnvId(ENV_ID).withUuid(TAG_ID).build();
    Host host = aHost().withUuid(HOST_ID).build();
    ServiceTemplate template = builder.withUuid(TEMPLATE_ID).withTags(asList(tag)).withLeafTags(singleton(tag)).build();

    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);
    when(tagService.getLeafTags(tag)).thenReturn(asList(tag));
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(tag))).thenReturn(asList(host));

    templateService.updateTags(APP_ID, ENV_ID, TEMPLATE_ID, asList());

    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID, of("tags", asList(), "leafTags", emptySet()));
    verify(serviceInstanceService).updateInstanceMappings(template, asList(), asList(host));
  }

  /**
   * Should add and delete tags.
   */
  @Test
  public void shouldAddAndDeleteTags() {
    Tag existingTag = aTag().withEnvId(ENV_ID).withUuid("EXISTING_TAG_ID").build();
    Host existingTagHost = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID_1").build();
    Tag newTag = aTag().withEnvId(ENV_ID).withUuid("NEW_TAG_ID").build();
    Host newTagHost = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID_2").build();
    ServiceTemplate template =
        builder.withUuid(TEMPLATE_ID).withTags(asList(existingTag)).withLeafTags(singleton(existingTag)).build();

    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);
    when(tagService.get(APP_ID, ENV_ID, "NEW_TAG_ID")).thenReturn(newTag);
    when(tagService.getLeafTags(existingTag)).thenReturn(asList(existingTag));
    when(tagService.getLeafTags(newTag)).thenReturn(asList(newTag));
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(existingTag))).thenReturn(asList(existingTagHost));
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(newTag))).thenReturn(asList(newTagHost));

    templateService.updateTags(APP_ID, ENV_ID, TEMPLATE_ID, asList("NEW_TAG_ID"));

    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID, of("tags", asList(newTag), "leafTags", singleton(newTag)));
    verify(serviceInstanceService).updateInstanceMappings(template, asList(newTagHost), asList(existingTagHost));
  }

  /**
   * Should fetch templates by tag.
   */
  @Test
  public void shouldFetchTemplatesByTag() {
    Tag tag = aTag().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TAG_ID).build();
    templateService.getTemplatesByLeafTag(tag);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field("leafTags");
    verify(end).equal(TAG_ID);
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

  /**
   * Should compute config files for hosts.
   */
  @Test
  public void shouldComputeConfigFilesForHosts() {
    when(wingsPersistence.get(ServiceTemplate.class, TEMPLATE_ID))
        .thenReturn(builder.withHosts(asList(aHost().withUuid("HOST_ID_1").build())).build());

    when(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_1").withName("PROPERTIES_FILE").build()));

    when(configService.getConfigFilesForEntity("TEMPLATE_ID", ENV_ID))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_2").withName("PROPERTIES_FILE").build()));

    when(configService.getConfigFilesForEntity("TEMPLATE_ID", "HOST_ID_1"))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_3").withName("PROPERTIES_FILE").build()));

    Map<String, List<ConfigFile>> hostConfigFileMapping =
        templateService.computedConfigFiles(APP_ID, ENV_ID, TEMPLATE_ID);
    assertThat(hostConfigFileMapping.get("HOST_ID_1"))
        .isEqualTo(asList(aConfigFile().withUuid("FILE_ID_3").withName("PROPERTIES_FILE").build()));
  }
}
