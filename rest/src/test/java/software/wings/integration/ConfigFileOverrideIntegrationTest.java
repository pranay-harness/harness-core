package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.ConfigFile.Builder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.integration.IntegrationTestUtil.randomInt;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure;
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
import software.wings.service.intfc.SettingsService;

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
@Ignore
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

  @Inject SettingsService settingsService;
  /**
   * The Template.
   */
  ServiceTemplate template;
  /**
   * The Hosts.
   */
  List<Host> hosts;

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setUp() throws IOException {
    // DB cleanup
    Arrays
        .asList(Application.class, Environment.class, Host.class, Infrastructure.class, ConfigFile.class,
            ServiceTemplate.class, Service.class, SettingAttribute.class)
        .forEach(aClass -> wingsPersistence.getDatastore().getCollection(aClass).drop());

    String accountId = wingsPersistence.save(anAccount().withCompanyName("Wings Software").build());

    settingsService.save(aSettingAttribute()
                             .withIsPluginSetting(true)
                             .withName("AppDynamics")
                             .withAccountId(accountId)
                             .withValue(AppDynamicsConfig.Builder.anAppDynamicsConfig()
                                            .withControllerUrl("https://na774.saas.appdynamics.com/controller")
                                            .withUsername("testuser")
                                            .withAccountname("na774")
                                            .withPassword("testuser123")
                                            .build())
                             .build());

    // test setup
    Application app = appService.save(anApplication().withName("AppA").build());
    Service service = srs.save(Service.Builder.aService().withAppId(app.getUuid()).withName("Catalog").build());
    List<Environment> environments = environmentService.getEnvByApp(app.getUuid());
    for (int i = 1; i < environments.size(); i++) {
      environmentService.delete(app.getUuid(), environments.get(i).getUuid());
    }
    Environment environment = environments.get(0);
    String infraId = infrastructureService.getInfraByEnvId(environment.getAppId(), environment.getUuid()).getUuid();

    hosts = importAndGetHosts(app.getUuid(), environment.getUuid(), infraId); // FIXME split

    template = templateService.save(aServiceTemplate()
                                        .withAppId(app.getUuid())
                                        .withServiceId(service.getUuid())
                                        .withEnvId(environment.getUuid())
                                        .withName("Catalog:8080")
                                        .build());
    template = templateService.get(template.getAppId(), template.getEnvId(), template.getUuid(), false);

    // add hosts and tags to template
    List<String> selectedHosts = Arrays.asList(hosts.get(8).getUuid(), hosts.get(9).getUuid());
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

    assertThat(hostConfigMapping.get(hosts.get(0).getHostName()))
        .isEqualTo(configService.getConfigFilesForEntity(
            template.getAppId(), DEFAULT_TEMPLATE_ID, template.getService().getUuid()));
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
      appConfigFile.setTemplateId(DEFAULT_TEMPLATE_ID);
      appConfigFile.setTargetToAllEnv(true);
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
                        .withHostConnAttr(settingAttribute.getUuid())
                        .withBastionConnAttr(settingAttribute.getUuid())
                        .build();
    //    int numOfHostsImported =
    //        hostService.importHosts(baseHost, new BoundedInputStream(new
    //        FileInputStream(createHostsFile(sshRoot.newFile("host.csv"), 10))));
    List<String> hostNames = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      hostNames.add(String.format("host%s.app.com", i));
    }
    // TODO:: HOST refactoring
    // baseHost.setHostNames(hostNames);
    hostService.bulkSave(infraId, envId, baseHost);
    //    log().info("{} host imported", numOfHostsImported);
    PageRequest<Host> pageRequest = new PageRequest<>();
    pageRequest.addFilter("infraId", infraId, EQ);
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
