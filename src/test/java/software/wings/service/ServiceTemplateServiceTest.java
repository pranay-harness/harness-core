package software.wings.service;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_SET;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ConfigFile.Builder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.Tag.Builder.aTag;
import static software.wings.beans.infrastructure.ApplicationHost.Builder.anApplicationHost;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
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
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.beans.Tag.TagType;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.beans.infrastructure.Infrastructure.InfrastructureType;
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
import software.wings.service.intfc.TagService;

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
  @Mock private TagService tagService;
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
                                                .withDescription(TEMPLATE_DESCRIPTION)
                                                .withMappedBy(EntityType.HOST);

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
    pageResponse.setResponse(asList(builder.but().withServiceId(SERVICE_ID).withHostIds(asList(HOST_ID)).build()));

    when(wingsPersistence.query(ServiceTemplate.class, pageRequest)).thenReturn(pageResponse);
    when(hostService.list(any(PageRequest.class))).thenReturn(aPageResponse().withResponse(asList(host)).build());

    PageResponse<ServiceTemplate> templatePageResponse = templateService.list(pageRequest, true);

    ServiceTemplate expectedServiceTemplate =
        builder.but().withServiceId(SERVICE_ID).withHostIds(asList(HOST_ID)).build();
    expectedServiceTemplate.setService(
        aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build());
    expectedServiceTemplate.setHosts(asList(host));

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
   * Should add hosts.
   */
  @Test
  public void shouldAddHosts() {
    ApplicationHost host = anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).build();
    when(hostService.get(APP_ID, ENV_ID, HOST_ID)).thenReturn(host);
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(builder.but().build());

    templateService.updateHosts(APP_ID, ENV_ID, TEMPLATE_ID, asList(HOST_ID));
    verify(wingsPersistence)
        .updateFields(
            ServiceTemplate.class, TEMPLATE_ID, of("mappedBy", EntityType.HOST, "hostIds", asList(host.getUuid())));
    ServiceTemplate template = builder.but().build();
    template.setService(aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build());
    verify(serviceInstanceService)
        .updateInstanceMappings(template,
            asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).build()), asList());
  }

  /**
   * Should delete hosts.
   */
  @Test
  public void shouldDeleteHosts() {
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID))
        .thenReturn(builder.withHostIds(asList(HOST_ID)).build());
    when(hostService.list(any(PageRequest.class)))
        .thenReturn(
            aPageResponse()
                .withResponse(asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).build()))
                .build());

    templateService.updateHosts(APP_ID, ENV_ID, TEMPLATE_ID, asList());
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID, of("mappedBy", EntityType.HOST, "hostIds", asList()));
    ServiceTemplate template = builder.but().build();
    template.setService(aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build());
    template.setHosts(asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).build()));
    verify(serviceInstanceService)
        .updateInstanceMappings(template, asList(),
            asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).build()));
  }

  /**
   * Should add and delete hosts.
   */
  @Test
  public void shouldAddAndDeleteHosts() {
    when(hostService.get(APP_ID, ENV_ID, "NEW_HOST_ID"))
        .thenReturn(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid("NEW_HOST_ID").build());
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID))
        .thenReturn(builder.but().withHostIds(asList("EXISTING_HOST_ID")).build());

    when(hostService.list(any(PageRequest.class)))
        .thenReturn(
            aPageResponse()
                .withResponse(asList(
                    anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid("EXISTING_HOST_ID").build()))
                .build());

    ServiceTemplate template = builder.but().withHostIds(asList("EXISTING_HOST_ID")).build();
    template.setService(aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build());
    template.setHosts(
        asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid("EXISTING_HOST_ID").build()));

    templateService.updateHosts(APP_ID, ENV_ID, TEMPLATE_ID, asList("NEW_HOST_ID"));
    verify(wingsPersistence)
        .updateFields(
            ServiceTemplate.class, TEMPLATE_ID, of("mappedBy", EntityType.HOST, "hostIds", asList("NEW_HOST_ID")));
    verify(serviceInstanceService)
        .updateInstanceMappings(template,
            asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid("NEW_HOST_ID").build()),
            asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid("EXISTING_HOST_ID").build()));
  }

  /**
   * Should add tags.
   */
  @Test
  public void shouldAddTags() {
    Tag tag = aTag().withEnvId(ENV_ID).withUuid(TAG_ID).build();
    Host host = aHost().withUuid(HOST_ID).build();
    ServiceTemplate template = builder.withMappedBy(EntityType.TAG).withUuid(TEMPLATE_ID).build();
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);
    when(tagService.get(APP_ID, ENV_ID, TAG_ID, true)).thenReturn(tag);
    when(tagService.getLeafTagInSubTree(tag)).thenReturn(asList(tag));
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(tag)))
        .thenReturn(
            asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).withHost(host).build()));

    templateService.updateTags(APP_ID, ENV_ID, TEMPLATE_ID, asList(TAG_ID));

    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID,
            of("mappedBy", EntityType.TAG, "tagIds", asList(tag.getUuid()), "leafTagIds", asList(tag.getUuid())));
    verify(serviceInstanceService)
        .updateInstanceMappings(template,
            asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).withHost(host).build()),
            asList());
  }

  /**
   * Should set mapped by to tag and remove mapped hosts when mapped by tag.
   */
  @Test
  public void shouldSetMappedByToTagAndRemoveMappedHostsWhenMappedByTag() {
    Tag tag = aTag().withEnvId(ENV_ID).withUuid(TAG_ID).build();
    ApplicationHost host = ApplicationHost.Builder.anApplicationHost().withUuid(HOST_ID).build();
    ServiceTemplate template = builder.but().withUuid(TEMPLATE_ID).withHostIds(asList(HOST_ID)).build();

    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);

    when(tagService.get(APP_ID, ENV_ID, TAG_ID, true)).thenReturn(tag);
    when(tagService.getLeafTagInSubTree(tag)).thenReturn(asList(tag));
    when(hostService.list(any(PageRequest.class))).thenReturn(aPageResponse().withResponse(asList(host)).build());
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(tag)))
        .thenReturn(asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).build()));

    doReturn(builder.withHosts(asList()).build())
        .when(spyTemplateService)
        .updateHosts(APP_ID, ENV_ID, TEMPLATE_ID, emptyList());

    spyTemplateService.updateTags(APP_ID, ENV_ID, TEMPLATE_ID, asList(TAG_ID));

    verify(spyTemplateService).updateHosts(APP_ID, ENV_ID, TEMPLATE_ID, emptyList());
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID,
            of("mappedBy", EntityType.TAG, "tagIds", asList(tag.getUuid()), "leafTagIds", asList(tag.getUuid())));

    verify(serviceInstanceService)
        .updateInstanceMappings(builder.withMappedBy(EntityType.TAG).build(),
            asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).build()), asList());
  }

  /**
   * Should set mapped by to host and remove mapped tags when mapped by host.
   */
  @Test
  public void shouldSetMappedByToHostAndRemoveMappedTagsWhenMappedByHost() {
    Tag tag = aTag().withEnvId(ENV_ID).withUuid(TAG_ID).build();
    ApplicationHost host = anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).build();
    ServiceTemplate template = builder.withMappedBy(EntityType.TAG).withUuid(TEMPLATE_ID).withTags(asList(tag)).build();

    when(infrastructureService.getInfraByEnvId(APP_ID, ENV_ID))
        .thenReturn(
            Infrastructure.Builder.anInfrastructure().withType(InfrastructureType.STATIC).withUuid(INFRA_ID).build());
    when(hostService.get(APP_ID, ENV_ID, HOST_ID)).thenReturn(host);
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);
    doReturn(builder.withTags(EMPTY_LIST).withLeafTags(EMPTY_SET).build())
        .when(spyTemplateService)
        .updateTags(APP_ID, ENV_ID, TEMPLATE_ID, EMPTY_LIST);
    when(tagService.list(any(PageRequest.class), eq(false)))
        .thenReturn(aPageResponse().withResponse(asList(tag)).build());

    spyTemplateService.updateHosts(APP_ID, ENV_ID, TEMPLATE_ID, asList(HOST_ID));

    verify(spyTemplateService).updateTags(APP_ID, ENV_ID, TEMPLATE_ID, emptyList());
    verify(serviceInstanceService)
        .updateInstanceMappings(builder.withMappedBy(EntityType.HOST).build(), asList(host), EMPTY_LIST);
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, template.getUuid(),
            of("mappedBy", EntityType.HOST, "hostIds", asList(host.getUuid())));
  }

  /**
   * Should delete tags.
   */
  @Test
  public void shouldDeleteTags() {
    Tag tag = aTag().withEnvId(ENV_ID).withUuid(TAG_ID).build();
    Host host = aHost().withUuid(HOST_ID).build();
    ServiceTemplate template = builder.withMappedBy(EntityType.TAG)
                                   .withUuid(TEMPLATE_ID)
                                   .withTagIds(asList(tag.getUuid()))
                                   .withLeafTagIds(asList(tag.getUuid()))
                                   .build();

    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);
    when(tagService.getLeafTagInSubTree(tag)).thenReturn(asList(tag));
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(tag)))
        .thenReturn(
            asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).withHost(host).build()));

    when(tagService.list(any(PageRequest.class), eq(false)))
        .thenReturn(aPageResponse().withResponse(asList(tag)).build());
    templateService.updateTags(APP_ID, ENV_ID, TEMPLATE_ID, asList());

    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID,
            of("mappedBy", EntityType.TAG, "tagIds", asList(), "leafTagIds", asList()));
    verify(serviceInstanceService)
        .updateInstanceMappings(template, asList(),
            asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).withHost(host).build()));
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
    ServiceTemplate template = builder.withMappedBy(EntityType.TAG)
                                   .withUuid(TEMPLATE_ID)
                                   .withTags(asList(existingTag))
                                   .withLeafTags(singleton(existingTag))
                                   .build();

    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(template);
    when(tagService.get(APP_ID, ENV_ID, "NEW_TAG_ID", true)).thenReturn(newTag);
    when(tagService.getLeafTagInSubTree(existingTag)).thenReturn(asList(existingTag));
    when(tagService.getLeafTagInSubTree(newTag)).thenReturn(asList(newTag));
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(existingTag)))
        .thenReturn(asList(anApplicationHost()
                               .withAppId(APP_ID)
                               .withEnvId(ENV_ID)
                               .withUuid(HOST_ID)
                               .withHost(existingTagHost)
                               .build()));
    when(hostService.getHostsByTags(APP_ID, ENV_ID, asList(newTag)))
        .thenReturn(asList(
            anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).withHost(newTagHost).build()));

    when(tagService.list(any(PageRequest.class), eq(false)))
        .thenReturn(aPageResponse().withResponse(asList(existingTag)).build());

    templateService.updateTags(APP_ID, ENV_ID, TEMPLATE_ID, asList("NEW_TAG_ID"));

    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID,
            of("mappedBy", EntityType.TAG, "tagIds", asList(newTag.getUuid()), "leafTagIds", asList(newTag.getUuid())));
    verify(serviceInstanceService)
        .updateInstanceMappings(template,
            asList(
                anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).withHost(newTagHost).build()),
            asList(anApplicationHost()
                       .withAppId(APP_ID)
                       .withEnvId(ENV_ID)
                       .withUuid(HOST_ID)
                       .withHost(existingTagHost)
                       .build()));
  }

  /**
   * Should fetch templates by tag.
   */
  @Test
  public void shouldFetchTemplatesByTag() {
    Tag tag = aTag().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TAG_ID).build();
    templateService.getTemplatesByLeafTag(tag.getUuid(), tag.getAppId(), tag.getEnvId());
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field("leafTagIds");
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
    when(wingsPersistence.get(ServiceTemplate.class, TEMPLATE_ID)).thenReturn(builder.build());

    when(configService.getConfigFilesForEntity(APP_ID, DEFAULT_TEMPLATE_ID, SERVICE_ID))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_1").withName("PROPERTIES_FILE").build()));

    when(configService.getConfigFilesForEntity(APP_ID, "TEMPLATE_ID", "HOST_ID_1"))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_3").withName("PROPERTIES_FILE").build()));

    when(tagService.getRootConfigTag(APP_ID, ENV_ID))
        .thenReturn(aTag()
                        .withTagType(TagType.ENVIRONMENT)
                        .withChildren(asList(aTag().withUuid(TAG_ID).withTagType(TagType.UNTAGGED_HOST).build()))
                        .build());
    when(hostService.getHostsByTags(any(), any(), anyList()))
        .thenReturn(asList(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid("HOST_ID_1").build()));

    Map<String, List<ConfigFile>> hostConfigFileMapping =
        templateService.computedConfigFiles(APP_ID, ENV_ID, TEMPLATE_ID);
    assertThat(hostConfigFileMapping.get("HOST_ID_1"))
        .isEqualTo(asList(aConfigFile().withUuid("FILE_ID_3").withName("PROPERTIES_FILE").build()));
  }
}
