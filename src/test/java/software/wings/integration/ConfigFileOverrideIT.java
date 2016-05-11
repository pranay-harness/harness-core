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
import static software.wings.integration.IntegrationTestUtil.createHostsFile;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.utils.HostFileHelper.HostFileType.CSV;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
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
import software.wings.rules.Integration;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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

@Integration
public class ConfigFileOverrideIT extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Inject AppService appService;
  @Inject ServiceResourceService srs;
  @Inject InfraService infraService;
  @Inject ServiceTemplateService templateService;
  @Inject TagService tagService;
  @Inject ConfigService configService;
  @Inject EnvironmentService environmentService;
  @Inject HostService hostService;

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

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
  public void setUp() throws IOException {
    // DB cleanup
    Arrays.asList(Host.class, Tag.class, TagType.class, ConfigFile.class, ServiceTemplate.class, Service.class)
        .forEach(aClass -> wingsPersistence.getDatastore().getCollection(aClass).drop());

    // test setup
    Application app = appService.save(anApplication().withName("AppA").build());
    Service service = srs.save(Service.ServiceBuilder.aService().withAppId(app.getUuid()).withName("Catalog").build());
    Environment environment = environmentService.save(anEnvironment().withAppId(app.getUuid()).withName("DEV").build());
    Infra infra = infraService.save(anInfra().withEnvId(environment.getUuid()).withInfraType(STATIC).build());

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
    hostService.tag(app.getUuid(), infra.getUuid(), hosts.get(0).getUuid(), ncOz1.getUuid());
    hostService.tag(app.getUuid(), infra.getUuid(), hosts.get(1).getUuid(), ncOz1.getUuid());
    hostService.tag(app.getUuid(), infra.getUuid(), hosts.get(2).getUuid(), ncOz1.getUuid());
    hostService.tag(app.getUuid(), infra.getUuid(), hosts.get(3).getUuid(), ncOz3.getUuid());
    hostService.tag(app.getUuid(), infra.getUuid(), hosts.get(4).getUuid(), ncOz3.getUuid());
    hostService.tag(app.getUuid(), infra.getUuid(), hosts.get(5).getUuid(), ncOz3.getUuid());

    template = templateService.save(
        aServiceTemplate().withService(service).withEnvId(environment.getUuid()).withName("Catalog:8080").build());
    log().info("Template id {}", template.getUuid());

    // add hosts and tags to template
    List<String> selectedTags = Arrays.asList(ncOz1.getUuid(), ncOz2.getUuid(), ncOz3.getUuid());
    List<String> selectedHosts = Arrays.asList(hosts.get(8).getUuid(), hosts.get(9).getUuid());
    templateService.updateHostAndTags(
        app.getUuid(), environment.getUuid(), template.getUuid(), selectedTags, selectedHosts);
  }

  @Test
  public void shouldApplyServiceConfigFilesIT() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), DEFAULT_TEMPLATE_ID);

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, template.getService().getUuid()));
  }

  @Test
  public void shouldApplyEnvConfigFileOverride() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), DEFAULT_TEMPLATE_ID);
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(9).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), template.getEnvId()));
  }

  @Test
  public void shouldApplyHostConfigsOverrideForTaggedHost() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), DEFAULT_TEMPLATE_ID);
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());
    attacheConfigFileToEntity(ncOz1.getUuid(), template.getUuid());
    attacheConfigFileToEntity(hosts.get(0).getUuid(), template.getUuid());
    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(1).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), ncOz1.getUuid()));
  }

  @Test
  public void shouldApplyHostConfigsOverrideForUntaggedHost() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), DEFAULT_TEMPLATE_ID);
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());
    attacheConfigFileToEntity(ncOz1.getUuid(), template.getUuid());
    attacheConfigFileToEntity(hosts.get(0).getUuid(), template.getUuid());

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), hosts.get(0).getUuid()));
  }

  @Test
  public void shouldApplyTagConfigFileOverride() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), DEFAULT_TEMPLATE_ID);
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());
    attacheConfigFileToEntity(ncOz1.getUuid(), template.getUuid());
    attacheConfigFileToEntity(hosts.get(8).getUuid(), template.getUuid());

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(8).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), hosts.get(8).getUuid()));
  }

  @Test
  public void shouldOverrideConfigFilesInAllScenariosIT() throws IOException {
    // prepare config files
    attacheConfigFileToEntity(template.getService().getUuid(), DEFAULT_TEMPLATE_ID);

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

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), hosts.get(0).getUuid()));

    assertThat(hostConfigMapping.get(hosts.get(1).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), ncOz1.getUuid()));

    assertThat(hostConfigMapping.get(hosts.get(8).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), hosts.get(8).getUuid()));

    assertThat(hostConfigMapping.get(hosts.get(9).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), template.getEnvId()));
  }

  private void attacheConfigFileToEntity(String entityId, String templateId) throws IOException {
    saveConfigFile(entityId, templateId, "app.properties");
    saveConfigFile(entityId, templateId, "cache.xml");
  }

  private void saveConfigFile(String entityId, String templateId, String fileName) throws IOException {
    ConfigFile appConfigFile = aConfigFile()
                                   .withName(fileName)
                                   .withTemplateId(templateId)
                                   .withEntityId(entityId)
                                   .withRelativePath("/configs/")
                                   .build();
    FileInputStream fileInputStream = new FileInputStream(createRandomFile());
    configService.save(appConfigFile, fileInputStream);
    fileInputStream.close();
    log().info("Attached config file [{}, {}] to entity uuid = {}", appConfigFile.getUuid(), appConfigFile.getName(),
        entityId);
  }

  private List<Host> importAndGetHosts(Infra infra) throws IOException {
    int numOfHostsImported = hostService.importHosts(infra.getAppId(), infra.getUuid(),
        new FileInputStream(createHostsFile(testFolder.newFile("host.csv"), 10)), CSV);
    log().info("{} host imported", numOfHostsImported);
    PageRequest<Host> pageRequest = new PageRequest<>();
    pageRequest.addFilter("infraId", infra.getUuid(), EQ);
    pageRequest.addFilter("appId", infra.getAppId(), EQ);
    return hostService.list(pageRequest).getResponse();
  }

  private File createRandomFile() throws IOException {
    File file = testFolder.newFile("randomfile " + randomInt());
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write("RandomText " + randomInt());
    out.close();
    return file;
  }
}
