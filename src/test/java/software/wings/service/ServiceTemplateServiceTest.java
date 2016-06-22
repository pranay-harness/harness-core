package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ConfigFile.ConfigFileBuilder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.Tag.TagBuilder.aTag;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.INFRA_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import com.google.common.collect.ImmutableMap;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.Host;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
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
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ConfigService configService;
  @Mock private TagService tagService;
  @Mock private HostService hostService;
  @Mock private ServiceInstanceService serviceInstanceService;

  @InjectMocks @Inject private ServiceTemplateService templateService;

  private ServiceTemplate.Builder builder = aServiceTemplate()
                                                .withUuid(TEMPLATE_ID)
                                                .withAppId(APP_ID)
                                                .withEnvId(ENV_ID)
                                                .withService(aService().withUuid(SERVICE_ID).build())
                                                .withName(TEMPLATE_NAME)
                                                .withDescription(TEMPLATE_DESCRIPTION);

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
   * Should update service template.
   */
  @Test
  public void shouldUpdateServiceTemplate() {
    ServiceTemplate template = builder.build();
    templateService.update(template);
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, TEMPLATE_ID,
            ImmutableMap.of("name", TEMPLATE_NAME, "description", TEMPLATE_DESCRIPTION, "service",
                aService().withUuid(SERVICE_ID).build()));
  }

  @Test
  public void shouldAddHosts() {
    Host host = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID").build();
    when(hostService.get(APP_ID, INFRA_ID, HOST_ID)).thenReturn(host);
    when(hostService.getInfraId(ENV_ID, APP_ID)).thenReturn(INFRA_ID);
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID)).thenReturn(builder.build());

    ServiceTemplate template = builder.build();
    templateService.updateHosts(APP_ID, ENV_ID, template.getUuid(), asList(HOST_ID));
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, template.getUuid(), ImmutableMap.of("hosts", asList(host)));
    verify(serviceInstanceService).updateInstanceMappings(template, asList(host), asList());
  }

  @Test
  public void shouldDeleteHosts() {
    Host host = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID").build();
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID))
        .thenReturn(builder.withHosts(asList(host)).build());

    ServiceTemplate template = builder.build();
    templateService.updateHosts(APP_ID, ENV_ID, template.getUuid(), asList());
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, template.getUuid(), ImmutableMap.of("hosts", asList()));
    verify(serviceInstanceService).updateInstanceMappings(template, asList(), asList(host));
  }

  @Test
  public void shouldAddAndDeleteHosts() {
    Host existingHost = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID_1").build();
    Host newHost = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withUuid("HOST_ID_2").build();
    when(hostService.get(APP_ID, INFRA_ID, "HOST_ID_2")).thenReturn(newHost);
    when(hostService.getInfraId(ENV_ID, APP_ID)).thenReturn(INFRA_ID);
    when(wingsPersistence.get(ServiceTemplate.class, APP_ID, TEMPLATE_ID))
        .thenReturn(builder.withHosts(asList(existingHost)).build());

    ServiceTemplate template = builder.build();
    templateService.updateHosts(APP_ID, ENV_ID, template.getUuid(), asList("HOST_ID_2"));
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, template.getUuid(), ImmutableMap.of("hosts", asList(newHost)));
    verify(serviceInstanceService).updateInstanceMappings(template, asList(newHost), asList(existingHost));
  }

  @Test
  public void shouldAddTags() {}

  @Test
  public void shouldDeleteTags() {}

  @Test
  public void shouldAddAndDeleteTags() {}

  /**
   * Should update host and tags.
   */
  @Test
  @Ignore
  public void shouldUpdateHostAndTags() {
    when(tagService.saveTag(eq("PARENT_TAG"), any(Tag.class))).thenReturn(aTag().withUuid("TAG_ID").build());
    when(hostService.save(any(Host.class))).thenReturn(aHost().withUuid("HOST_ID").build());
    when(tagService.getTag(APP_ID, "TAG_ID")).thenReturn(aTag().withUuid("TAG_ID").build());
    when(hostService.get(APP_ID, INFRA_ID, "HOST_ID")).thenReturn(aHost().withUuid("HOST_ID").build());
    when(wingsPersistence.get(ServiceTemplate.class, TEMPLATE_ID))
        .thenReturn(aServiceTemplate().withUuid("SERVICE_TEMPLATE").build());

    ServiceTemplate template = builder.build();
    Tag tag = tagService.saveTag("PARENT_TAG", aTag().build());
    Host host = hostService.save(any(Host.class));
    templateService.updateHosts(APP_ID, ENV_ID, template.getUuid(), asList(host.getUuid()));
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, template.getUuid(), ImmutableMap.of("hosts", asList(host)));
    templateService.updateTags(APP_ID, ENV_ID, template.getUuid(), asList(tag.getUuid()));
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, template.getUuid(), ImmutableMap.of("tags", asList(tag)));
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
