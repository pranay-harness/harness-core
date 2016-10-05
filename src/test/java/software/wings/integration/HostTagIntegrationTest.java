package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Tag.Builder.aTag;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.ErrorCodes;
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.beans.Tag.TagType;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
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
   * The Infrastructure service.
   */
  @Inject InfrastructureService infrastructureService;

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
   * The Infrastructure id.
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
        .asList(Application.class, Environment.class, Host.class, Infrastructure.class, Tag.class, ConfigFile.class,
            ServiceTemplate.class, Service.class, SettingAttribute.class)
        .forEach(aClass -> wingsPersistence.getDatastore().getCollection(aClass).drop());

    // test setup
    Application app = appService.save(anApplication().withName("AppA").build());
    List<Environment> environments = environmentService.getEnvByApp(app.getUuid());
    for (int i = 1; i < environments.size(); i++) {
      environmentService.delete(app.getUuid(), environments.get(i).getUuid());
    }
    environment = environments.get(0);
    infraId = infrastructureService.getInfraByEnvId(environment.getAppId(), environment.getUuid()).getUuid();

    // create Tag hierarchy
    rootEnvTag = tagService.getRootConfigTag(app.getUuid(), environment.getUuid());
    defaultTag = tagService.getDefaultTagForUntaggedHosts(rootEnvTag.getAppId(), rootEnvTag.getEnvId());

    nc = tagService.save(rootEnvTag.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("NC").build());
  }

  /**
   * Should add new hosts to default tag for untagged hosts.
   */
  @Test
  public void shouldAddNewHostsToDefaultTagForUntaggedHosts() {
    List<ApplicationHost> hosts = importAndGetHosts(environment.getAppId(), environment.getUuid(), infraId);
    hosts.forEach(host -> assertThat(host.getConfigTag().getTagType()).isEqualTo(TagType.UNTAGGED_HOST));
  }

  /**
   * Should have zero host with default tag when all host tagged by other tag.
   */
  @Test
  public void shouldHaveZeroHostWithDefaultTagWhenAllHostTaggedByOtherTag() {
    List<ApplicationHost> hosts = importAndGetHosts(environment.getAppId(), environment.getUuid(), infraId);
    tagService.tagHosts(nc, hosts);
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    hosts.forEach(host -> assertThat(host.getConfigTag()).isEqualTo(nc));
    assertThat(hosts.stream().filter(host -> host.getConfigTag().getTagType().equals(TagType.UNTAGGED_HOST)).count())
        .isEqualTo(0);
  }

  /**
   * Should remove default tag from host when tagged by other tags.
   */
  @Test
  public void shouldRemoveDefaultTagFromHostWhenTaggedByOtherTags() {
    List<ApplicationHost> hosts = importAndGetHosts(environment.getAppId(), environment.getUuid(), infraId);
    List<ApplicationHost> hostsToTag = Arrays.asList(hosts.get(0), hosts.get(1), hosts.get(2));
    tagService.tagHosts(nc, hostsToTag);
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    final List<ApplicationHost> finalHosts = hosts;
    hostsToTag.forEach(host
        -> assertThat(
            finalHosts.stream().filter(h -> h.getUuid().equals(host.getUuid())).findFirst().get().getConfigTag())
               .isEqualTo(nc));
    assertThat(hosts.stream().filter(host -> host.getConfigTag().getTagType().equals(TagType.UNTAGGED_HOST)).count())
        .isEqualTo(2);
  }

  /**
   * Should tag host by default tag when untagged.
   */
  @Test
  public void shouldTagHostByDefaultTagWhenUntagged() {
    List<ApplicationHost> hosts = importAndGetHosts(environment.getAppId(), environment.getUuid(), infraId);
    tagService.tagHosts(nc, hosts);
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    hosts.forEach(host -> assertThat(host.getConfigTag()).isEqualTo(nc));
    tagService.tagHosts(nc, Arrays.asList());
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    hosts.forEach(host -> assertThat(host.getConfigTag()).isEqualTo(defaultTag));
  }

  private List<ApplicationHost> importAndGetHosts(String appId, String envId, String infraId) {
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
    hostService.bulkSave(infraId, envId, baseHost);
    PageRequest<ApplicationHost> pageRequest = new PageRequest<>();
    pageRequest.addFilter("infraId", infraId, EQ);
    return hostService.list(pageRequest).getResponse();
  }

  @Test
  public void shouldThrowExceptionIfHostMappedToNonLeafTags() {
    List<ApplicationHost> hosts = importAndGetHosts(environment.getAppId(), environment.getUuid(), infraId);
    tagService.save(nc.getUuid(),
        aTag().withAppId(rootEnvTag.getAppId()).withEnvId(rootEnvTag.getEnvId()).withName("NC_OZ1").build());
    assertThatThrownBy(()
                           -> tagService.tagHostsByApi(
                               nc.getAppId(), nc.getEnvId(), nc.getUuid(), Arrays.asList(hosts.get(0).getUuid())))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCodes.INVALID_REQUEST.name());
  }
}
