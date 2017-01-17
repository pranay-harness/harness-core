package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping.Builder;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/16/17.
 */
@RealMongo
public class InfrastructureMappingIntegrationTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private EnvironmentService environmentService;
  @Inject private HostService hostService;
  @Inject private SettingsService settingsService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  private Application app;
  private Service service;
  private Environment environment;

  @Before
  public void setUp() throws Exception {
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
    app = appService.save(anApplication().withName("AppA").build());
    service =
        serviceResourceService.save(Service.Builder.aService().withAppId(app.getUuid()).withName("Catalog").build());
    List<Environment> environments = environmentService.getEnvByApp(app.getUuid());
    for (int i = 1; i < environments.size(); i++) {
      environmentService.delete(app.getUuid(), environments.get(i).getUuid());
    }
    environment = environments.get(0);
  }

  @Test
  public void shouldSelectServiceInstances() {
    List<Key<ServiceTemplate>> templateRefKeysByService =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), service.getUuid(), environment.getUuid());
    String serviceTemplateId = (String) templateRefKeysByService.get(0).getId();

    SettingAttribute hostConnectionAttr =
        wingsPersistence.saveAndGet(SettingAttribute.class, aSettingAttribute().withAppId(app.getUuid()).build());
    SettingAttribute computeProviderSetting =
        wingsPersistence.saveAndGet(SettingAttribute.class, aSettingAttribute().withAppId(app.getUuid()).build());

    PhysicalInfrastructureMapping physicalInfrastructureMapping =
        Builder.aPhysicalInfrastructureMapping()
            .withAppId(app.getUuid())
            .withEnvId(environment.getUuid())
            .withServiceTemplateId(serviceTemplateId)
            .withHostConnectionAttrs(hostConnectionAttr.getUuid())
            .withComputeProviderSettingId(computeProviderSetting.getUuid())
            .withHostnames(Arrays.asList("host1", "host2"))
            .build();
    PhysicalInfrastructureMapping infrastructureMapping =
        (PhysicalInfrastructureMapping) infrastructureMappingService.save(physicalInfrastructureMapping);

    assertThat(infrastructureMapping.getHostnames()).containsExactlyInAnyOrder("host1", "host2");
    assertThat(serviceInstanceService.list(new PageRequest<>())
                   .getResponse()
                   .stream()
                   .map(ServiceInstance::getHostName)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder("host1", "host2");

    // Setup done. Two Host and Two service instances

    List<ServiceInstance> serviceInstances =
        infrastructureMappingService.selectServiceInstances(app.getUuid(), service.getUuid(), environment.getUuid(),
            infrastructureMapping.getComputeProviderSettingId(), ImmutableMap.of());

    assertThat(serviceInstances.size()).isEqualTo(2);
    assertThat(serviceInstances.stream().map(ServiceInstance::getHostName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("host1", "host2");
  }
}
