package software.wings.integration;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsConfig.Builder.anAwsConfig;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.ConnectionType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/16/17.
 */
public class InfrastructureMappingIntegrationTest extends BaseIntegrationTest {
  @Mock private JobScheduler jobScheduler;
  @Inject @InjectMocks private AppService appService;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  private Application app;
  private Service service;
  private Environment environment;

  @Before
  public void setUp() throws Exception {
    deleteAllDocuments(
        asList(Application.class, Service.class, SettingAttribute.class, ServiceInstance.class, Host.class));

    app = appService.save(anApplication().withName("AppA").withAccountId(accountId).build());
    service =
        serviceResourceService.save(Service.Builder.aService().withAppId(app.getUuid()).withName("Catalog").build());

    // test setup
    List<Environment> environments = environmentService.getEnvByApp(app.getUuid());
    for (int i = 1; i < environments.size(); i++) {
      environmentService.delete(app.getUuid(), environments.get(i).getUuid());
    }
    environment = environments.get(0);
  }

  @Test
  public void shouldSelectServiceInstances() {
    SettingAttribute hostConnectionAttr = wingsPersistence.saveAndGet(
        SettingAttribute.class, aSettingAttribute().withAccountId(app.getAccountId()).withName("hca").build());
    SettingAttribute computeProviderSetting = wingsPersistence.saveAndGet(
        SettingAttribute.class, aSettingAttribute().withAccountId(app.getAccountId()).withName("DC").build());

    String serviceTemplateId = (String) serviceTemplateService
                                   .getTemplateRefKeysByService(app.getUuid(), service.getUuid(), environment.getUuid())
                                   .get(0)
                                   .getId();

    PhysicalInfrastructureMapping physicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withAppId(app.getUuid())
            .withEnvId(environment.getUuid())
            .withServiceTemplateId(serviceTemplateId)
            .withDeploymentType(DeploymentType.SSH.name())
            .withComputeProviderType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
            .withHostConnectionAttrs(hostConnectionAttr.getUuid())
            .withComputeProviderSettingId(computeProviderSetting.getUuid())
            .withHostNames(asList("host1", "host2"))
            .build();
    PhysicalInfrastructureMapping infrastructureMapping =
        (PhysicalInfrastructureMapping) infrastructureMappingService.save(physicalInfrastructureMapping);

    assertThat(infrastructureMapping.getHostNames()).containsExactlyInAnyOrder("host1", "host2");
    assertThat(serviceInstanceService.list(new PageRequest<>())
                   .getResponse()
                   .stream()
                   .map(ServiceInstance::getHostName)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder("host1", "host2");

    // Setup done. Two Host and Two service instances

    // Test specific host
    List<ServiceInstance> serviceInstances = infrastructureMappingService.selectServiceInstances(app.getUuid(),
        environment.getUuid(), infrastructureMapping.getUuid(),
        aServiceInstanceSelectionParams().withSelectSpecificHosts(true).withHostNames(asList("host1")).build());

    assertThat(serviceInstances).hasSize(1).extracting(ServiceInstance::getHostName).containsExactlyInAnyOrder("host1");

    // Test host count
    serviceInstances = infrastructureMappingService.selectServiceInstances(app.getUuid(), environment.getUuid(),
        infrastructureMapping.getUuid(),
        aServiceInstanceSelectionParams().withSelectSpecificHosts(false).withCount(2).build());

    assertThat(serviceInstances)
        .hasSize(2)
        .extracting(ServiceInstance::getHostName)
        .containsExactlyInAnyOrder("host1", "host2");

    // Test exclusion
    List<ServiceInstance> allServiceInstances = serviceInstanceService.list(new PageRequest<>()).getResponse();

    // Exclude 1st service instance
    serviceInstances = infrastructureMappingService.selectServiceInstances(app.getUuid(), environment.getUuid(),
        infrastructureMapping.getUuid(),
        aServiceInstanceSelectionParams()
            .withSelectSpecificHosts(false)
            .withCount(2)
            .withExcludedServiceInstanceIds(asList(allServiceInstances.get(0).getUuid()))
            .build());
    assertThat(serviceInstances).hasSize(1).containsExactly(allServiceInstances.get(1));

    // Exclude 2nd service instance
    serviceInstances = infrastructureMappingService.selectServiceInstances(app.getUuid(), environment.getUuid(),
        infrastructureMapping.getUuid(),
        aServiceInstanceSelectionParams()
            .withSelectSpecificHosts(false)
            .withCount(2)
            .withExcludedServiceInstanceIds(asList(allServiceInstances.get(1).getUuid()))
            .build());
    assertThat(serviceInstances).hasSize(1).containsExactly(allServiceInstances.get(0));

    // Exclude both service instance
    serviceInstances = infrastructureMappingService.selectServiceInstances(app.getUuid(), environment.getUuid(),
        infrastructureMapping.getUuid(),
        aServiceInstanceSelectionParams()
            .withSelectSpecificHosts(false)
            .withCount(2)
            .withExcludedServiceInstanceIds(
                allServiceInstances.stream().map(ServiceInstance::getUuid).collect(Collectors.toList()))
            .build());
    assertThat(serviceInstances).hasSize(0);
  }

  @Test
  @Ignore
  public void shouldSelectAwsInfrastructureInstances() {
    List<Key<ServiceTemplate>> templateRefKeysByService =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), service.getUuid(), environment.getUuid());
    String serviceTemplateId = (String) templateRefKeysByService.get(0).getId();

    SettingAttribute hostConnectionAttr = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute()
            .withAppId(app.getUuid())
            .withValue(HostConnectionAttributes.Builder.aHostConnectionAttributes()
                           .withAccessType(AccessType.KEY)
                           .withConnectionType(ConnectionType.SSH)
                           .withKey("wingsKey".toCharArray())
                           .build())
            .build());
    SettingAttribute computeProviderSetting = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute()
            .withAppId(app.getUuid())
            .withValue(anAwsConfig()
                           .withAccessKey("AKIAJLEKM45P4PO5QUFQ")
                           .withSecretKey("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".toCharArray())
                           .build())
            .build());

    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping()
            .withAppId(app.getUuid())
            .withEnvId(environment.getUuid())
            .withServiceTemplateId(serviceTemplateId)
            .withComputeProviderSettingId(computeProviderSetting.getUuid())
            .withHostConnectionAttrs(hostConnectionAttr.getUuid())
            .build();

    awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMappingService.save(awsInfrastructureMapping);

    //    List<ServiceInstance> serviceInstances = infrastructureMappingService
    //        .selectServiceInstances(app.getUuid(), environment.getUuid(), awsInfrastructureMapping.getUuid(),
    //        ImmutableMap.of(), new ArrayList<>());

    //    System.out.println(serviceInstances.size());
    //    serviceInstances = infrastructureMappingService
    //        .selectServiceInstances(app.getUuid(), environment.getUuid(), awsInfrastructureMapping.getUuid(),
    //        ImmutableMap.of(), new ArrayList<>());
    //    System.out.println(serviceInstances.size());

    System.out.println(awsInfrastructureMapping.toString());
  }

  @Test
  @Ignore
  public void shouldProvisionAwsNodes() {
    List<Key<ServiceTemplate>> templateRefKeysByService =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), service.getUuid(), environment.getUuid());
    String serviceTemplateId = (String) templateRefKeysByService.get(0).getId();

    SettingAttribute hostConnectionAttr = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute()
            .withAppId(app.getUuid())
            .withValue(HostConnectionAttributes.Builder.aHostConnectionAttributes()
                           .withAccessType(AccessType.KEY)
                           .withConnectionType(ConnectionType.SSH)
                           .withKey("wingsKey".toCharArray())
                           .build())
            .build());
    SettingAttribute computeProviderSetting = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute()
            .withAppId(app.getUuid())
            .withValue(anAwsConfig()
                           .withAccessKey("AKIAJLEKM45P4PO5QUFQ")
                           .withSecretKey("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".toCharArray())
                           .build())
            .build());

    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping()
            .withAppId(app.getUuid())
            .withEnvId(environment.getUuid())
            .withServiceTemplateId(serviceTemplateId)
            .withComputeProviderSettingId(computeProviderSetting.getUuid())
            .withHostConnectionAttrs(hostConnectionAttr.getUuid())
            .build();

    awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMappingService.save(awsInfrastructureMapping);

    List<ServiceInstance> serviceInstances = infrastructureMappingService.provisionNodes(
        app.getUuid(), environment.getUuid(), awsInfrastructureMapping.getUuid(), "DemoTargetHosts", 5);

    System.out.println(serviceInstances.size());
    //    serviceInstances = infrastructureMappingService
    //        .selectServiceInstances(app.getUuid(), environment.getUuid(), awsInfrastructureMapping.getUuid(),
    //        ImmutableMap.of(), new ArrayList<>());
    System.out.println(serviceInstances.size());

    System.out.println(awsInfrastructureMapping.toString());
  }
}
