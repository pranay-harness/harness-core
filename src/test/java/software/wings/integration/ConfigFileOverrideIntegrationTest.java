package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.ConfigFile.Builder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Host.Builder.aHost;
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
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
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

/**
 * Created by anubhaw on 4/28/16.
 */

/**
 * Test setup
 * ------------------------
 * Service
 * |
 * |....ENV (ROOT_TAG)
 * |    |...NC
 * |    |   |....NC_OZ1---[Host0, Host1, Host2]
 * |    |   |....NC_OZ2
 * |    |   |....NC_OZ3---[Host3, Host4, Host5]
 * |    |
 * |    |...OR
 * |    |   |...OR_OZ1
 * |    |   |...OR_OZ2
 * |    |
 * |    |....ENV-UNTAGGED_HOSTS
 * |    |    |...Host8
 * |    |    |...Host9
 * |
 */
@RealMongo
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
   * The Infrastructure service.
   */
  @Inject InfrastructureService infrastructureService;
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
   * The Root env tag.
   */
  Tag rootEnvTag;

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
    Arrays
        .asList(Application.class, Environment.class, Host.class, Infrastructure.class, Tag.class, ConfigFile.class,
            ServiceTemplate.class, Service.class, SettingAttribute.class)
        .forEach(aClass -> wingsPersistence.getDatastore().getCollection(aClass).drop());

    // test setup
    Application app = appService.save(anApplication().withName("AppA").build());
    Service service = srs.save(Service.Builder.aService().withAppId(app.getUuid()).withName("Catalog").build());
    Environment environment = environmentService.getEnvByApp(app.getUuid()).get(0);
    String infraId = infrastructureService.getInfraByEnvId(environment.getAppId(), environment.getUuid()).getUuid();

    hosts = importAndGetHosts(app.getUuid(), environment.getUuid(), infraId); // FIXME split

    // create Tag hierarchy
    rootEnvTag = tagService.getRootConfigTag(app.getUuid(), environment.getUuid());

    nc = tagService.save(rootEnvTag.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("NC").build());
    ncOz1 = tagService.save(nc.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("NC_OZ1").build());
    ncOz2 = tagService.save(nc.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("NC_OZ2").build());
    ncOz3 = tagService.save(nc.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("NC_OZ3").build());

    or = tagService.save(rootEnvTag.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("OR").build());
    orOz1 = tagService.save(or.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("OR_OZ1").build());
    orOz2 = tagService.save(or.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("OR_OZ2").build());

    // Tag hosts
    tagService.tagHosts(ncOz1, Arrays.asList(hosts.get(0), hosts.get(1), hosts.get(2)));
    tagService.tagHosts(ncOz3, Arrays.asList(hosts.get(3), hosts.get(4), hosts.get(5)));

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
    attacheConfigFileToEntity(template.getService().getUuid(), EntityType.SERVICE);

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(
            template.getAppId(), DEFAULT_TEMPLATE_ID, template.getService().getUuid()));
  }

  /**
   * Should apply host configs override for tagged host.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldApplyHostConfigsOverrideForTaggedHost() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), EntityType.SERVICE);
    attacheConfigFileToEntity(rootEnvTag.getUuid(), EntityType.TAG);
    attacheConfigFileToEntity(ncOz1.getUuid(), EntityType.TAG);
    attacheConfigFileToEntity(hosts.get(0).getUuid(), EntityType.TAG);
    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(1).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getAppId(), template.getUuid(), ncOz1.getUuid()));
  }

  /**
   * Should apply host configs override for untagged host.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldApplyHostConfigsOverrideForUntaggedHost() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), EntityType.SERVICE);
    attacheConfigFileToEntity(rootEnvTag.getUuid(), EntityType.TAG);
    attacheConfigFileToEntity(ncOz1.getUuid(), EntityType.TAG);
    attacheConfigFileToEntity(hosts.get(0).getUuid(), EntityType.TAG);

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(
            configService.getConfigFilesForEntity(template.getAppId(), template.getUuid(), hosts.get(0).getUuid()));
  }

  /**
   * Should apply tag config file override.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldApplyTagConfigFileOverride() throws IOException {
    attacheConfigFileToEntity(template.getService().getUuid(), EntityType.SERVICE);
    attacheConfigFileToEntity(rootEnvTag.getUuid(), EntityType.TAG);
    attacheConfigFileToEntity(ncOz1.getUuid(), EntityType.TAG);
    attacheConfigFileToEntity(hosts.get(8).getUuid(), EntityType.HOST);

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(8).getUuid()))
        .isEqualTo(
            configService.getConfigFilesForEntity(template.getAppId(), template.getUuid(), hosts.get(8).getUuid()));
  }

  /**
   * Should override config files in all scenarios it.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldOverrideConfigFilesInAllScenariosIT() throws IOException {
    // prepare config files
    attacheConfigFileToEntity(template.getService().getUuid(), EntityType.SERVICE);

    // rootEnvTag
    attacheConfigFileToEntity(rootEnvTag.getUuid(), EntityType.TAG);

    // ncOz1
    attacheConfigFileToEntity(ncOz1.getUuid(), EntityType.TAG);

    // ncOz3
    attacheConfigFileToEntity(ncOz3.getUuid(), EntityType.TAG);

    // tagged host
    attacheConfigFileToEntity(hosts.get(0).getUuid(), EntityType.HOST);
    attacheConfigFileToEntity(hosts.get(3).getUuid(), EntityType.HOST);

    // untagged host
    attacheConfigFileToEntity(hosts.get(8).getUuid(), EntityType.HOST);

    Map<String, List<ConfigFile>> hostConfigMapping =
        templateService.computedConfigFiles(template.getAppId(), template.getEnvId(), template.getUuid());

    assertThat(hostConfigMapping.get(hosts.get(0).getUuid()))
        .isEqualTo(
            configService.getConfigFilesForEntity(template.getAppId(), template.getUuid(), hosts.get(0).getUuid()));

    assertThat(hostConfigMapping.get(hosts.get(1).getUuid()))
        .isEqualTo(configService.getConfigFilesForEntity(template.getAppId(), template.getUuid(), ncOz1.getUuid()));

    assertThat(hostConfigMapping.get(hosts.get(8).getUuid()))
        .isEqualTo(
            configService.getConfigFilesForEntity(template.getAppId(), template.getUuid(), hosts.get(8).getUuid()));

    assertThat(hostConfigMapping.get(hosts.get(9).getUuid()))
        .isEqualTo(
            configService.getConfigFilesForEntity(template.getAppId(), template.getUuid(), rootEnvTag.getUuid()));
  }

  private void attacheConfigFileToEntity(String entityId, EntityType entityType) throws IOException {
    saveConfigFile(entityId, entityType, "app.properties");
    saveConfigFile(entityId, entityType, "cache.xml");
  }

  private void saveConfigFile(String entityId, EntityType entityType, String fileName) throws IOException {
    ConfigFile appConfigFile = aConfigFile()
                                   .withAppId(template.getAppId())
                                   .withEnvId(template.getEnvId())
                                   .withName(fileName)
                                   .withFileName(fileName)
                                   .withTemplateId(template.getUuid())
                                   .withEntityId(entityId)
                                   .withEntityType(entityType)
                                   .withRelativeFilePath("configs/" + fileName)
                                   .build();
    if (entityType == EntityType.SERVICE) {
      appConfigFile.setEnvId(GLOBAL_ENV_ID);
    }
    FileInputStream fileInputStream = new FileInputStream(createRandomFile());
    configService.save(appConfigFile, fileInputStream);
    fileInputStream.close();
    log().info("Attached config file [{}, {}] to entity uuid = {}", appConfigFile.getUuid(), appConfigFile.getName(),
        entityId);
  }

  private List<Host> importAndGetHosts(String appId, String envId, String infraId) {
    SettingAttribute settingAttribute =
        wingsPersistence.saveAndGet(SettingAttribute.class, aSettingAttribute().withAppId(appId).build());
    Host baseHost = aHost()
                        .withAppId(appId)
                        .withInfraId(infraId)
                        .withHostConnAttr(settingAttribute)
                        .withBastionConnAttr(settingAttribute)
                        .build();
    //    int numOfHostsImported =
    //        hostService.importHosts(baseHost, new BoundedInputStream(new
    //        FileInputStream(createHostsFile(sshRoot.newFile("host.csv"), 10))));
    List<String> hostNames = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      hostNames.add(String.format("host%s.app.com", i));
    }
    baseHost.setHostNames(hostNames);
    hostService.bulkSave(envId, baseHost);
    //    log().info("{} host imported", numOfHostsImported);
    PageRequest<Host> pageRequest = new PageRequest<>();
    pageRequest.addFilter("infraId", infraId, EQ);
    pageRequest.addFilter("appId", appId, EQ);
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
