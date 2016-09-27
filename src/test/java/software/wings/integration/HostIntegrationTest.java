package software.wings.integration;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.ApplicationHost;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.Service;
import software.wings.beans.Service.Builder;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.WingsPersistence;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 9/27/16.
 */
@RealMongo
public class HostIntegrationTest extends WingsBaseTest {
  /**
   * The Test folder.
   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();
  /**
   * The Wings persistence.
   */
  @Inject private WingsPersistence wingsPersistence;
  /**
   * The App service.
   */
  @Inject private AppService appService;

  @Inject private ServiceResourceService serviceResourceService;

  @Inject private ServiceTemplateService serviceTemplateService;

  @Inject private ServiceInstanceService serviceInstanceService;

  /**
   * The Infrastructure service.
   */
  @Inject private InfrastructureService infrastructureService;

  /**
   * The Environment service.
   */
  @Inject private EnvironmentService environmentService;
  /**
   * The Host service.
   */
  @Inject private HostService hostService;

  /**
   * The Infrastructure id.
   */
  String infraId;

  /**
   * The Environment.
   */
  Environment environment;

  /**
   * The Order service template.
   */
  ServiceTemplate orderServiceTemplate;
  /**
   * The Account service template.
   */
  ServiceTemplate accountServiceTemplate;

  /**
   * The Setting attribute.
   */
  SettingAttribute settingAttribute;

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setUp() throws IOException {
    // DB cleanup
    asList(Application.class, Environment.class, Host.class, Infrastructure.class, Tag.class, ConfigFile.class,
        ServiceTemplate.class, Service.class, SettingAttribute.class)
        .forEach(aClass -> wingsPersistence.getDatastore().getCollection(aClass).drop());

    // test setup
    Application app = appService.save(anApplication().withName("AppA").build());
    environment = environmentService.getEnvByApp(app.getUuid()).get(0);

    Service account =
        serviceResourceService.save(Builder.aService().withAppId(app.getAppId()).withName("Account").build());
    accountServiceTemplate = serviceTemplateService.get(app.getUuid(),
        (String) serviceTemplateService
            .getTemplateRefKeysByService(app.getUuid(), environment.getUuid(), account.getUuid())
            .get(0)
            .getId());
    Service order = serviceResourceService.save(Builder.aService().withAppId(app.getAppId()).withName("Order").build());
    orderServiceTemplate = serviceTemplateService.get(app.getUuid(),
        (String) serviceTemplateService
            .getTemplateRefKeysByService(app.getUuid(), environment.getUuid(), order.getUuid())
            .get(0)
            .getId());

    infraId = infrastructureService.getDefaultInfrastructureId();

    settingAttribute =
        wingsPersistence.saveAndGet(SettingAttribute.class, aSettingAttribute().withAppId(app.getUuid()).build());
  }

  /**
   * Should bulk save host and map service templates.
   */
  @Test
  public void shouldBulkSaveHostAndMapServiceTemplates() {
    Host baseHost = aHost()
                        .withAppId(environment.getAppId())
                        .withInfraId(infraId)
                        .withHostConnAttr(settingAttribute)
                        .withBastionConnAttr(settingAttribute)
                        .build();
    List<String> hostNames = asList("host1", "host2", "host3");
    baseHost.setHostNames(hostNames);
    baseHost.setServiceTemplates(asList(orderServiceTemplate));
    hostService.bulkSave(infraId, environment.getUuid(), baseHost);
    assertThat(hostService.getHostCountByInfrastructure(infraId)).isEqualTo(3);
    assertThat(hostService.getHostsByEnv(environment.getAppId(), environment.getUuid())).hasSize(3);
    assertThat(serviceTemplateService.get(orderServiceTemplate.getAppId(), orderServiceTemplate.getUuid())
                   .getHosts()
                   .stream()
                   .map(ApplicationHost::getHostName)
                   .collect(Collectors.toList()))
        .containsAll(hostNames);

    assertThat(serviceInstanceService
                   .list(aPageRequest()
                             .addFilter("appId", EQ, environment.getAppId())
                             .addFilter("envId", EQ, environment.getUuid())
                             .build())
                   .getResponse()
                   .size())
        .isEqualTo(3);
  }

  /**
   * Should map existing hosts to new service template in add host flow.
   */
  @Test
  public void shouldMapExistingHostsToNewServiceTemplateInAddHostFlow() {
    Host baseHost = aHost()
                        .withAppId(environment.getAppId())
                        .withInfraId(infraId)
                        .withHostConnAttr(settingAttribute)
                        .withBastionConnAttr(settingAttribute)
                        .build();
    List<String> hostNames = asList("host1", "host2", "host3");
    baseHost.setHostNames(hostNames);
    baseHost.setServiceTemplates(asList(orderServiceTemplate));
    hostService.bulkSave(infraId, environment.getUuid(), baseHost);

    // reinsert old host with new service template mapping
    baseHost.setServiceTemplates(asList(accountServiceTemplate));
    hostService.bulkSave(infraId, environment.getUuid(), baseHost);

    assertThat(hostService.getHostCountByInfrastructure(infraId)).isEqualTo(3);
    assertThat(hostService.getHostsByEnv(environment.getAppId(), environment.getUuid())).hasSize(3);
    assertThat(serviceTemplateService.get(orderServiceTemplate.getAppId(), orderServiceTemplate.getUuid())
                   .getHosts()
                   .stream()
                   .map(ApplicationHost::getHostName)
                   .collect(Collectors.toList()))
        .containsAll(hostNames);
    assertThat(serviceTemplateService.get(accountServiceTemplate.getAppId(), orderServiceTemplate.getUuid())
                   .getHosts()
                   .stream()
                   .map(ApplicationHost::getHostName)
                   .collect(Collectors.toList()))
        .containsAll(hostNames);
    assertThat(serviceInstanceService
                   .list(aPageRequest()
                             .addFilter("appId", EQ, environment.getAppId())
                             .addFilter("envId", EQ, environment.getUuid())
                             .build())
                   .getResponse()
                   .size())
        .isEqualTo(6);
  }

  /**
   * Should only create application host for existing infra host when re added.
   */
  @Test
  public void shouldOnlyCreateApplicationHostForExistingInfraHostWhenReAdded() {
    Host baseHost = aHost()
                        .withAppId(environment.getAppId())
                        .withInfraId(infraId)
                        .withHostConnAttr(settingAttribute)
                        .withBastionConnAttr(settingAttribute)
                        .build();
    List<String> hostNames = asList("host1", "host2", "host3");
    baseHost.setHostNames(hostNames);
    baseHost.setServiceTemplates(asList(orderServiceTemplate));
    hostService.bulkSave(infraId, environment.getUuid(), baseHost);
    List<ApplicationHost> applicationHosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());
    hostService.delete(environment.getAppId(), environment.getUuid(), applicationHosts.get(0).getUuid());

    assertThat(hostService.getHostsByEnv(environment.getAppId(), environment.getUuid()))
        .hasSize(2); // 2 applicationHosts
    assertThat(hostService.getHostCountByInfrastructure(infraId)).isEqualTo(3); // 3 infraHosts
    assertThat(serviceInstanceService
                   .list(aPageRequest()
                             .addFilter("appId", EQ, environment.getAppId())
                             .addFilter("envId", EQ, environment.getUuid())
                             .build())
                   .getResponse()
                   .size())
        .isEqualTo(2);

    hostService.bulkSave(infraId, environment.getUuid(), baseHost);

    assertThat(hostService.getHostsByEnv(environment.getAppId(), environment.getUuid()))
        .hasSize(3); // 3 applicationHosts
    assertThat(hostService.getHostCountByInfrastructure(infraId)).isEqualTo(3); // 3 infraHosts
    assertThat(serviceInstanceService
                   .list(aPageRequest()
                             .addFilter("appId", EQ, environment.getAppId())
                             .addFilter("envId", EQ, environment.getUuid())
                             .build())
                   .getResponse()
                   .size())
        .isEqualTo(3);
  }

  @Test
  public void shouldDeleteAllServiceTemplateMappingAndAllServiceInstancesOnHostDeleteByInfra() {
    Host baseHost = aHost()
                        .withAppId(environment.getAppId())
                        .withInfraId(infraId)
                        .withHostConnAttr(settingAttribute)
                        .withBastionConnAttr(settingAttribute)
                        .build();
    List<String> hostNames = asList("host1", "host2", "host3");
    baseHost.setHostNames(hostNames);
    baseHost.setServiceTemplates(asList(orderServiceTemplate));
    hostService.bulkSave(infraId, environment.getUuid(), baseHost);
    hostService.deleteByInfra(infraId);

    assertThat(hostService.getHostCountByInfrastructure(infraId)).isEqualTo(3);
    assertThat(hostService.getHostsByEnv(environment.getAppId(), environment.getUuid())).hasSize(0);
    assertThat(serviceInstanceService
                   .list(aPageRequest()
                             .addFilter("appId", EQ, environment.getAppId())
                             .addFilter("envId", EQ, environment.getUuid())
                             .build())
                   .getResponse()
                   .size())
        .isEqualTo(0);
    assertThat(serviceInstanceService
                   .list(aPageRequest()
                             .addFilter("appId", EQ, environment.getAppId())
                             .addFilter("envId", EQ, environment.getUuid())
                             .build())
                   .getResponse()
                   .size())
        .isEqualTo(0);
  }
}
