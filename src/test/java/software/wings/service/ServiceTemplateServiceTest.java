package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ConfigFile.ConfigFileBuilder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.Service.ServiceBuilder.aService;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.beans.Tag.TagBuilder.aTag;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.ConfigFile;
import software.wings.beans.Host;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceTemplate.ServiceTemplateBuilder;
import software.wings.beans.Tag;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ServiceTemplateServiceImpl;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 4/29/16.
 */

public class ServiceTemplateServiceTest {
  ServiceTemplateService templateService;

  private InfraService infraService = mock(InfraService.class);

  private HostService hostService = mock(HostService.class);

  private TagService tagService = mock(TagService.class);

  private ConfigService configService = mock(ConfigService.class);

  private WingsPersistence wingsPersistence = mock(WingsPersistence.class);

  ServiceTemplateBuilder builder = aServiceTemplate()
                                       .withUuid("TEMPLATE_ID")
                                       .withEnvId("ENV_ID")
                                       .withService(aService().withUuid("SERVICE_ID").build())
                                       .withName("TEMPLATE_NAME")
                                       .withDescription("TEMPLATE_DESCRIPTION");

  @Before
  public void setUp() throws Exception {
    templateService = new ServiceTemplateServiceImpl(wingsPersistence, tagService, configService);
  }

  @Test
  public void shouldListSavedServiceTemplates() {
    PageResponse<ServiceTemplate> pageResponse = new PageResponse<>();
    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(builder.build()));
    when(wingsPersistence.query(ServiceTemplate.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ServiceTemplate> templates = templateService.list(pageRequest);
    assertThat(templates).containsAll(asList(builder.build()));
  }

  @Test
  public void shouldSaveServiceTemplate() {
    when(wingsPersistence.saveAndGet(eq(ServiceTemplate.class), any(ServiceTemplate.class)))
        .thenReturn(builder.build());
    ServiceTemplate template = templateService.save(builder.build());
    assertThat(template.getName()).isEqualTo("TEMPLATE_NAME");
    assertThat(template.getService().getUuid()).isEqualTo("SERVICE_ID");
  }

  @Test
  public void shouldUpdateServiceTemplate() {
    ServiceTemplate template = builder.build();
    templateService.update(template);
    verify(wingsPersistence)
        .updateFields(ServiceTemplate.class, "TEMPLATE_ID",
            ImmutableMap.of("name", "TEMPLATE_NAME", "description", "TEMPLATE_DESCRIPTION", "service",
                aService().withUuid("SERVICE_ID").build()));
  }

  @Test
  public void shouldUpdateHostAndTags() {
    when(tagService.saveTag(eq("PARENT_TAG"), any(Tag.class))).thenReturn(aTag().withUuid("TAG_ID").build());
    when(hostService.save(any(Host.class))).thenReturn(aHost().withUuid("HOST_ID").build());
    when(wingsPersistence.get(Tag.class, "TAG_ID")).thenReturn(aTag().withUuid("TAG_ID").build());
    when(wingsPersistence.get(Host.class, "HOST_ID")).thenReturn(aHost().withUuid("HOST_ID").build());

    ServiceTemplate template = builder.build();
    Tag tag = tagService.saveTag("PARENT_TAG", aTag().build());
    Host host = hostService.save(any(Host.class));

    templateService.updateHostAndTags(
        "APP_ID", "ENV_ID", template.getUuid(), asList(tag.getUuid()), asList(host.getUuid()));

    verify(wingsPersistence)
        .updateFields(
            ServiceTemplate.class, template.getUuid(), ImmutableMap.of("hosts", asList(host), "tags", asList(tag)));
  }

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
  public void shouldComputeConfigFilesForHosts() {
    when(wingsPersistence.get(ServiceTemplate.class, "TEMPLATE_ID"))
        .thenReturn(builder.withHosts(asList(aHost().withUuid("HOST_ID_1").build())).build());

    when(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, "SERVICE_ID"))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_1").withName("PROPERTIES_FILE").build()));

    when(configService.getConfigFilesForEntity("TEMPLATE_ID", "ENV_ID"))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_2").withName("PROPERTIES_FILE").build()));

    when(configService.getConfigFilesForEntity("TEMPLATE_ID", "HOST_ID_1"))
        .thenReturn(asList(aConfigFile().withUuid("FILE_ID_3").withName("PROPERTIES_FILE").build()));

    Map<String, List<ConfigFile>> hostConfigFileMapping =
        templateService.computedConfigFiles("APP_ID", "ENV_ID", "TEMPLATE_ID");
    assertThat(hostConfigFileMapping.get("HOST_ID_1"))
        .isEqualTo(asList(aConfigFile().withUuid("FILE_ID_3").withName("PROPERTIES_FILE").build()));
  }
}
