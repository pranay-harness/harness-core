package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.service.impl.StaticInfrastructureProvider;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.WingsTestConstants;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/10/17.
 */
public class InfrastructureMappingServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private Map<String, InfrastructureProvider> infrastructureProviders;
  @Mock private StaticInfrastructureProvider staticInfrastructureProvider;
  @Mock private AwsInfrastructureProvider awsInfrastructureProvider;

  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private SettingsService settingsService;
  @Mock private HostService hostService;
  @Mock private ExecutorService executorService;

  @Inject @InjectMocks private InfrastructureMappingService infrastructureMappingService;

  private SettingAttribute computeProviderSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig().build())
          .build();

  @Before
  public void setUp() throws Exception {
    when(infrastructureProviders.get(SettingVariableTypes.AWS.name())).thenReturn(awsInfrastructureProvider);
    when(infrastructureProviders.get(SettingVariableTypes.PHYSICAL_DATA_CENTER.name()))
        .thenReturn(staticInfrastructureProvider);
  }

  @Test
  public void shouldList() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .build();

    PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
    PageResponse pageResponse = aPageResponse().withResponse(asList(physicalInfrastructureMapping)).build();
    when(wingsPersistence.query(InfrastructureMapping.class, pageRequest)).thenReturn(pageResponse);

    PageResponse<InfrastructureMapping> infrastructureMappings = infrastructureMappingService.list(pageRequest);
    assertThat(infrastructureMappings).hasSize(1).containsExactly(physicalInfrastructureMapping);
    verify(wingsPersistence).query(InfrastructureMapping.class, pageRequest);
  }

  @Test
  public void shouldSave() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .withHostNames(asList(HOST_NAME))
                                                                      .build();
    PhysicalInfrastructureMapping savedPhysicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
            .withComputeProviderSettingId(SETTING_ID)
            .withUuid(WingsTestConstants.INFRA_MAPPING_ID)
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withServiceTemplateId(TEMPLATE_ID)
            .withHostNames(asList(HOST_NAME))
            .build();

    when(wingsPersistence.saveAndGet(InfrastructureMapping.class, physicalInfrastructureMapping))
        .thenReturn(savedPhysicalInfrastructureMapping);
    ServiceTemplate serviceTemplate = aServiceTemplate().withAppId(APP_ID).withUuid(TEMPLATE_ID).build();
    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(serviceTemplate);
    Host host = aHost()
                    .withAppId(APP_ID)
                    .withEnvId(ENV_ID)
                    .withServiceTemplateId(TEMPLATE_ID)
                    .withInfraMappingId(INFRA_MAPPING_ID)
                    .withUuid(HOST_ID)
                    .withHostName(HOST_NAME)
                    .build();
    when(staticInfrastructureProvider.saveHost(any(Host.class))).thenReturn(host);

    InfrastructureMapping returnedInfrastructureMapping =
        infrastructureMappingService.save(physicalInfrastructureMapping);

    assertThat(returnedInfrastructureMapping.getUuid()).isEqualTo(INFRA_MAPPING_ID);
    verify(wingsPersistence).saveAndGet(InfrastructureMapping.class, physicalInfrastructureMapping);
    verify(serviceTemplateService).get(APP_ID, TEMPLATE_ID);
    verify(wingsPersistence).updateField(InfrastructureMapping.class, INFRA_MAPPING_ID, "hostNames", asList(HOST_NAME));
    verify(staticInfrastructureProvider).saveHost(any(Host.class));
    verify(serviceInstanceService)
        .updateInstanceMappings(serviceTemplate, savedPhysicalInfrastructureMapping, asList(host), asList());
  }

  @Test
  public void shouldGet() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withUuid(INFRA_MAPPING_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .withHostNames(asList(HOST_NAME))
                                                                      .build();

    when(wingsPersistence.get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID))
        .thenReturn(physicalInfrastructureMapping);

    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(APP_ID, ENV_ID, INFRA_MAPPING_ID);
    assertThat(infrastructureMapping.getUuid()).isEqualTo(INFRA_MAPPING_ID);
    verify(wingsPersistence).get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);
  }

  @Test
  public void shouldUpdate() {}

  @Test
  public void shouldDelete() {
    infrastructureMappingService.delete(APP_ID, ENV_ID, INFRA_MAPPING_ID);
    verify(wingsPersistence).delete(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);
  }
}
