package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ConfigFile.ConfigFileBuilder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Environment.EnvironmentBuilder.anEnvironment;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.Infra.InfraBuilder.anInfra;
import static software.wings.beans.Infra.InfraType.STATIC;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Tag.Builder.aTag;
import static software.wings.integration.IntegrationTestUtil.randomInt;

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
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc
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
public class ConfigFileOverrideIntegrationTest extends WingsBaseTest {
  /**
   * The Test folder.
   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();
  /**
   * The Wings persistence.
   */
  @Inject WingsPersistence wingsPersistence;
  /**
   * The App service.
   */
  @Inject AppService appService;
  /**
   * The Srs.
   */
  @Inject ServiceResourceService srs;
  /**
   * The Infra service.
   */
  @Inject InfraService infraService;
  /**
   * The Template service.
   */
  @Inject ServiceTemplateService templateService;
  /**
   * The Tag service.
   */
  @Inject TagService tagService;
  /**
   * The Config service.
   */
  @Inject ConfigService configService;
  /**
   * The Environment service.
   */
  @Inject EnvironmentService environmentService;
  /**
   * The Host service.
   */
  @Inject HostService hostService;
  /**
   * The Template.
   */
  ServiceTemplate template;
  /**
   * The Hosts.
   */
  List<Host> hosts;

  /**
   * The Nc.
   */
  Tag nc;
  /**
   * The Nc oz 1.
   */
  Tag ncOz1;
  /**
   * The Nc oz 2.
   */
  Tag ncOz2;
  /**
   * The Nc oz 3.
   */
  Tag ncOz3;
  /**
   * The Or.
   */
  Tag or;
  /**
   * The Or oz 1.
   */
  Tag orOz1;
  /**
   * The Or oz 2.
   */
  Tag orOz2;

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setUp() throws IOException {
    // DB cleanup
    Arrays.asList(Host.class, Tag.class, ConfigFile.class, ServiceTemplate.class, Service.class, SettingAttribute.class)
        .forEach(aClass -> wingsPersistence.getDatastore().getCollection(aClass).drop());

    // test setup
    Application app = appService.save(anApplication().withName("AppA").build());
    Service service = srs.save(Service.Builder.aService().withAppId(app.getUuid()).withName("Catalog").build());
    Environment environment = environmentService.save(anEnvironment().withAppId(app.getUuid()).withName("DEV").build());
    Infra infra = infraService.save(
        anInfra().withAppId(app.getUuid()).withEnvId(environment.getUuid()).withInfraType(STATIC).build());

    hosts = importAndGetHosts(infra); // FIXME split

    // create Tag hierarchy
    Tag rootTag = tagService.getRootConfigTag(app.getUuid(), environment.getUuid());

    nc = tagService.save(
        rootTag.getUuid(), aTag().withAppId(rootTag.getAppId()).withEnvId(rootTag.getEnvId()).withName("NC").build());
    ncOz1 = tagService.save(
        nc.getUuid(), aTag().withAppId(rootTag.getAppId()).withEnvId(rootTag.getEnvId()).withName("NC_OZ1").build());
    ncOz2 = tagService.save(
        nc.getUuid(), aTag().withAppId(rootTag.getAppId()).withEnvId(rootTag.getEnvId()).withName("NC_OZ2").build());
    ncOz3 = tagService.save(
        nc.getUuid(), aTag().withAppId(rootTag.getAppId()).withEnvId(rootTag.getEnvId()).withName("NC_OZ3").build());

    or = tagService.save(
        rootTag.getUuid(), aTag().withAppId(rootTag.getAppId()).withEnvId(rootTag.getEnvId()).withName("OR").build());
    orOz1 = tagService.save(
        or.getUuid(), aTag().withAppId(rootTag.getAppId()).withEnvId(rootTag.getEnvId()).withName("OR_OZ1").build());
    orOz2 = tagService.save(
        or.getUuid(), aTag().withAppId(rootTag.getAppId()).withEnvId(rootTag.getEnvId()).withName("OR_OZ2").build());

    // Tag hosts
    tagService.tagHosts(app.getUuid(), rootTag.getEnvId(), ncOz1.getUuid(),
        Arrays.asList(hosts.get(0).getUuid(), hosts.get(1).getUuid(), hosts.get(2).getUuid()));
    tagService.tagHosts(app.getUuid(), rootTag.getEnvId(), ncOz3.getUuid(),
        Arrays.asList(hosts.get(3).getUuid(), hosts.get(4).getUuid(), hosts.get(5).getUuid()));

    template = templateService.save(aServiceTemplate()
                                        .withAppId(app.getUuid())
                                        .withService(service)
                                        .withEnvId(environment.getUuid())
                                        .withName("Catalog:8080")
                                        .build());
    log().info("Template id {}", template.getUuid());

    // add hosts and tags to template
    List<String> selectedTags = Arrays.asList(ncOz1.getUuid(), ncOz2.getUuid(), ncOz3.getUuid());
    List<String> selectedHosts = Arrays.asList(hosts.get(8).getUuid(), hosts.get(9).getUuid());
    templateService.updateHosts(app.getUuid(), template.getEnvId(), template.getUuid(), selectedHosts);
    templateService.updateTags(app.getUuid(), template.getEnvId(), template.getUuid(), selectedTags);
  }

  /**
   * Should apply service config files it.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldApplyServiceConfigFilesIT() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), DEFAULT_TEMPLATE_ID);

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, template.getService().getUuid()));
  }

  /**
   * Should apply env config file override.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldApplyEnvConfigFileOverride() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), DEFAULT_TEMPLATE_ID);
    attacheConfigFileToEntity(template.getEnvId(), template.getUuid());

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(9).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getUuid(), template.getEnvId()));
  }

  /**
   * Should apply host configs override for tagged host.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
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

  /**
   * Should apply host configs override for untagged host.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
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

  /**
   * Should apply tag config file override.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
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

  /**
   * Should override config files in all scenarios it.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
    SettingAttribute settingAttribute =
        wingsPersistence.saveAndGet(SettingAttribute.class, aSettingAttribute().withAppId(infra.getAppId()).build());
    Host baseHost = aHost()
                        .withAppId(infra.getAppId())
                        .withInfraId(infra.getUuid())
                        .withHostConnAttr(settingAttribute)
                        .withBastionConnAttr(settingAttribute)
                        .build();
    //    int numOfHostsImported =
    //        hostService.importHosts(baseHost, new BoundedInputStream(new
    //        FileInputStream(createHostsFile(testFolder.newFile("host.csv"), 10))));
    List<String> hostNames = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      hostNames.add(String.format("host%s.app.com", i));
    }
    hostService.bulkSave(baseHost, hostNames);
    //    log().info("{} host imported", numOfHostsImported);
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
