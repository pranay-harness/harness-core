package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ConfigFile.ConfigFileBuilder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Environment.EnvironmentBuilder.anEnvironment;
import static software.wings.beans.Infra.InfraBuilder.anInfra;
import static software.wings.beans.Infra.InfraType.STATIC;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.beans.Tag.TagBuilder.aTag;
import static software.wings.utils.HostFileHelper.HostFileType.CSV;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseIntegrationTest;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.PageRequest;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Tag;
import software.wings.beans.TagType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 4/28/16.
 */

/**
 * Test setup
 * ------------------------
 * Service
 * |
 * |....ENV
 * |    |...NC
 * |    |   |....NC_OZ1---[Host0, Host1, Host2]
 * |    |   |....NC_OZ2
 * |    |   |....NC_OZ3---[Host3, Host4, Host5]
 * |    |
 * |    |...OR
 * |    |   |...OR_OZ1
 * |    |   |...OR_OZ2
 * |
 * |...Host8
 * |...Host9
 * |
 */

public class ConfigFileOverrideIT extends WingsBaseIntegrationTest {
  @Inject WingsPersistence wingsPersistence;

  @Inject AppService appService;
  @Inject ServiceResourceService srs;
  @Inject InfraService infraService;
  @Inject ServiceTemplateService templateService;
  @Inject TagService tagService;
  @Inject ConfigService configService;
  @Inject EnvironmentService environmentService;

  ServiceTemplate template;
  List<Host> hosts;

  Tag nc;
  Tag ncOz1;
  Tag ncOz2;
  Tag ncOz3;
  Tag or;
  Tag orOz1;
  Tag orOz2;

  @Before
  public void setUp() throws FileNotFoundException {
    // DB cleanup
    Arrays.asList(Host.class, Tag.class, TagType.class, ConfigFile.class, ServiceTemplate.class, Service.class)
        .forEach(aClass -> wingsPersistence.getDatastore().getCollection(aClass).drop());

    // test setup
    Application application = appService.save(anApplication().withName("AppA").build());
    Service service = srs.save(application.getUuid(), Service.ServiceBuilder.aService().withName("Catalog").build());
    Environment environment = environmentService.save(application.getUuid(), anEnvironment().withName("DEV").build());
    Infra infra = infraService.save(anInfra().withInfraType(STATIC).build(), environment.getUuid());

    hosts = importAndGetHosts(infra); // FIXME split

    // create Tag hierarchy
    TagType tagType = wingsPersistence.createQuery(TagType.class).field("envId").equal(environment.getUuid()).get();

    nc = tagService.saveTag(aTag().withTagType(tagType).withName("NC").build());
    ncOz1 = tagService.saveTag(aTag().withTagType(tagType).withName("NC_OZ1").build());
    ncOz2 = tagService.saveTag(aTag().withTagType(tagType).withName("NC_OZ2").build());
    ncOz3 = tagService.saveTag(aTag().withTagType(tagType).withName("NC_OZ3").build());

    tagService.linkTags(nc.getUuid(), ncOz1.getUuid());
    tagService.linkTags(nc.getUuid(), ncOz2.getUuid());
    tagService.linkTags(nc.getUuid(), ncOz3.getUuid());

    or = tagService.saveTag(aTag().withTagType(tagType).withName("OR").build());
    orOz1 = tagService.saveTag(aTag().withTagType(tagType).withName("OR_OZ1").build());
    orOz2 = tagService.saveTag(aTag().withTagType(tagType).withName("OR_OZ2").build());

    tagService.linkTags(or.getUuid(), orOz1.getUuid());
    tagService.linkTags(or.getUuid(), orOz2.getUuid());

    // Tag hosts
    infraService.tagHost(hosts.get(0).getUuid(), ncOz1.getUuid());
    infraService.tagHost(hosts.get(1).getUuid(), ncOz1.getUuid());
    infraService.tagHost(hosts.get(2).getUuid(), ncOz1.getUuid());
    infraService.tagHost(hosts.get(3).getUuid(), ncOz3.getUuid());
    infraService.tagHost(hosts.get(4).getUuid(), ncOz3.getUuid());
    infraService.tagHost(hosts.get(5).getUuid(), ncOz3.getUuid());

    template = templateService.save(aServiceTemplate()
                                        .withServiceId(service.getUuid())
                                        .withEnvId(environment.getUuid())
                                        .withName("Catalog:8080")
                                        .build());
    log().info("Template id {}", template.getUuid());

    // add hosts and tags to template
    List<String> selectedTags = Arrays.asList(ncOz1.getUuid(), ncOz2.getUuid(), ncOz3.getUuid());
    List<String> selectedHosts = Arrays.asList(hosts.get(8).getUuid(), hosts.get(9).getUuid());
    templateService.updateHostAndTags(template.getUuid(), selectedTags, selectedHosts);
  }

  @Test
  public void shouldApplyServiceConfigFilesIT() throws IOException {
    attacheConfigFileToEntity(template.getServiceId(), DEFAULT_TEMPLATE_ID);

    Map<String, List<ConfigFile>> hostConfigMapping = templateService.computedConfigFiles(template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, template.getServiceId()));
  }

  @Test
  public void shouldApplyEnvConfigFileOverride() throws IOException {
    attacheConfigFileToEntity(template.getServiceId(), DEFAULT_TEMPLATE_ID);
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());

    Map<String, List<ConfigFile>> hostConfigMapping = templateService.computedConfigFiles(template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(9).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), template.getEnvId()));
  }

  @Test
  public void shouldApplyHostConfigsOverrideForTaggedHost() throws IOException {
    attacheConfigFileToEntity(template.getServiceId(), DEFAULT_TEMPLATE_ID);
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());
    attacheConfigFileToEntity(ncOz1.getUuid(), template.getUuid());
    attacheConfigFileToEntity(hosts.get(0).getUuid(), template.getUuid());
    Map<String, List<ConfigFile>> hostConfigMapping = templateService.computedConfigFiles(template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(1).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), ncOz1.getUuid()));
  }

  @Test
  public void shouldApplyHostConfigsOverrideForUntaggedHost() throws IOException {
    attacheConfigFileToEntity(template.getServiceId(), DEFAULT_TEMPLATE_ID);
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());
    attacheConfigFileToEntity(ncOz1.getUuid(), template.getUuid());
    attacheConfigFileToEntity(hosts.get(0).getUuid(), template.getUuid());

    Map<String, List<ConfigFile>> hostConfigMapping = templateService.computedConfigFiles(template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), hosts.get(0).getUuid()));
  }

  @Test
  public void shouldApplyTagConfigFileOverride() throws IOException {
    attacheConfigFileToEntity(template.getServiceId(), DEFAULT_TEMPLATE_ID);
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());
    attacheConfigFileToEntity(ncOz1.getUuid(), template.getUuid());
    attacheConfigFileToEntity(hosts.get(8).getUuid(), template.getUuid());

    Map<String, List<ConfigFile>> hostConfigMapping = templateService.computedConfigFiles(template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(8).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), hosts.get(8).getUuid()));
  }

  @Test
  public void shouldOverrideConfigFilesInAllScenariosIT() throws IOException {
    // prepare config files
    attacheConfigFileToEntity(template.getServiceId(), DEFAULT_TEMPLATE_ID);

    // env
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());

    // ncOz1
    attacheConfigFileToEntity(ncOz1.getUuid(), template.getUuid());

    // ncOz3
    attacheConfigFileToEntity(ncOz3.getUuid(), template.getUuid());

    // tagged host
    attacheConfigFileToEntity(hosts.get(0).getUuid(), template.getUuid());
    attacheConfigFileToEntity(hosts.get(3).getUuid(), template.getUuid());

    // untagged host
    attacheConfigFileToEntity(hosts.get(8).getUuid(), template.getUuid());

    Map<String, List<ConfigFile>> hostConfigMapping = templateService.computedConfigFiles(template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), hosts.get(0).getUuid()));

    assertThat(hostConfigMapping.get(hosts.get(1).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), ncOz1.getUuid()));

    assertThat(hostConfigMapping.get(hosts.get(8).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), hosts.get(8).getUuid()));

    assertThat(hostConfigMapping.get(hosts.get(9).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), template.getEnvId()));
  }

  int appFileIdx = 1;
  int cacheFileIdx = 1;
  String relativePath = System.getProperty("user.home") + "/data/config/";

  private void attacheConfigFileToEntity(String entityId, String templateId) throws IOException {
    String filePrefix = "app";
    String fileExt = ".properties";
    String filePath = relativePath + filePrefix + appFileIdx++ + fileExt;
    saveConfigFile(entityId, templateId, filePrefix, fileExt, filePath);

    filePrefix = "cache";
    fileExt = ".xml";
    filePath = relativePath + filePrefix + cacheFileIdx++ + fileExt;
    saveConfigFile(entityId, templateId, filePrefix, fileExt, filePath);
  }

  private void saveConfigFile(String entityId, String templateId, String filePrefix, String fileExt, String filePath)
      throws IOException {
    ConfigFile appConfigFile = aConfigFile()
                                   .withName(filePrefix + fileExt)
                                   .withTemplateId(templateId)
                                   .withEntityId(entityId)
                                   .withRelativePath(relativePath)
                                   .build();
    FileInputStream fileInputStream = new FileInputStream(filePath);
    configService.save(appConfigFile, fileInputStream);
    fileInputStream.close();
    log().info("Attached config file [{}, {}] to entity uuid = {}", appConfigFile.getUuid(), appConfigFile.getName(),
        entityId);
  }

  private List<Host> importAndGetHosts(Infra infra) throws FileNotFoundException {
    int numOfHostsImported = infraService.importHosts(
        infra.getUuid(), new FileInputStream(System.getProperty("user.home") + "/data/hosts.csv"), CSV);
    log().info("{} host imported", numOfHostsImported);
    PageRequest<Host> pageRequest = new PageRequest<>();
    pageRequest.addFilter("infraId", infra.getUuid(), EQ);
    return infraService.listHosts(pageRequest).getResponse();
  }
}
