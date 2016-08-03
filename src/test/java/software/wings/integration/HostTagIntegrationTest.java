package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Tag.Builder.aTag;

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
import software.wings.beans.Tag.TagType;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.TagService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 8/1/16.
 */

@RealMongo
public class HostTagIntegrationTest extends WingsBaseTest {
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
   * The Infra service.
   */
  @Inject InfraService infraService;

  /**
   * The Tag service.
   */
  @Inject TagService tagService;

  /**
   * The Environment service.
   */
  @Inject EnvironmentService environmentService;
  /**
   * The Host service.
   */
  @Inject HostService hostService;
  /**
   * The Root env tag.
   */
  Tag rootEnvTag;

  /**
   * The Default tag.
   */
  Tag defaultTag;

  /**
   * The Nc.
   */
  Tag nc;

  /**
   * The Infra id.
   */
  String infraId;

  /**
   * The Environment.
   */
  Environment environment;

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setUp() throws IOException {
    // DB cleanup
    Arrays
        .asList(Application.class, Environment.class, Host.class, Infra.class, Tag.class, ConfigFile.class,
            ServiceTemplate.class, Service.class, SettingAttribute.class)
        .forEach(aClass -> wingsPersistence.getDatastore().getCollection(aClass).drop());

    // test setup
    Application app = appService.save(anApplication().withName("AppA").build());
    environment = environmentService.getEnvByApp(app.getUuid()).get(0);
    infraId = infraService.getInfraIdByEnvId(environment.getAppId(), environment.getUuid());

    // create Tag hierarchy
    rootEnvTag = tagService.getRootConfigTag(app.getUuid(), environment.getUuid());
    defaultTag = tagService.getDefaultTagForUntaggedHosts(rootEnvTag.getAppId(), rootEnvTag.getEnvId());

    nc = tagService.save(rootEnvTag.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("NC").build());
  }

  @Test
  public void shouldAddNewHostsToDefaultTagForUntaggedHosts() {
    List<Host> hosts = importAndGetHosts(environment.getAppId(), environment.getUuid(), infraId);
    hosts.forEach(host -> assertThat(host.getTags().get(0).getTagType()).isEqualTo(TagType.UNTAGGED_HOST));
  }

  @Test
  public void shouldHaveZeroHostWithDefaultTagWhenAllHostTaggedByOtherTag() {
    List<Host> hosts = importAndGetHosts(environment.getAppId(), environment.getUuid(), infraId);
    tagService.tagHosts(nc, hosts);
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    hosts.forEach(host -> assertThat(host.getTags().get(0)).isEqualTo(nc));
    assertThat(hosts.stream().filter(host -> host.getTags().get(0).getTagType().equals(TagType.UNTAGGED_HOST)).count())
        .isEqualTo(0);
  }

  @Test
  public void shouldRemoveDefaultTagFromHostWhenTaggedByOtherTags() {
    List<Host> hosts = importAndGetHosts(environment.getAppId(), environment.getUuid(), infraId);
    List<Host> hostsToTag = Arrays.asList(hosts.get(0), hosts.get(1), hosts.get(2));
    tagService.tagHosts(nc, hostsToTag);
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    final List<Host> finalHosts = hosts;
    hostsToTag.forEach(host
        -> assertThat(
            finalHosts.stream().filter(h -> h.getUuid().equals(host.getUuid())).findFirst().get().getTags().get(0))
               .isEqualTo(nc));
    assertThat(hosts.stream().filter(host -> host.getTags().get(0).getTagType().equals(TagType.UNTAGGED_HOST)).count())
        .isEqualTo(2);
  }

  @Test
  public void shouldTagHostByDefaultTagWhenUntagged() {
    List<Host> hosts = importAndGetHosts(environment.getAppId(), environment.getUuid(), infraId);
    tagService.tagHosts(nc, hosts);
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    hosts.forEach(host -> assertThat(host.getTags().get(0)).isEqualTo(nc));
    tagService.tagHosts(nc, Arrays.asList());
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    hosts.forEach(host -> assertThat(host.getTags().get(0)).isEqualTo(defaultTag));
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
    List<String> hostNames = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      hostNames.add(String.format("host%s.app.com", i));
    }
    baseHost.setHostNames(hostNames);
    hostService.bulkSave(envId, baseHost);
    PageRequest<Host> pageRequest = new PageRequest<>();
    pageRequest.addFilter("infraId", infraId, EQ);
    pageRequest.addFilter("appId", appId, EQ);
    return hostService.list(pageRequest).getResponse();
  }
}
