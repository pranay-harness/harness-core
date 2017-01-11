package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.ConnectionType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.utils.WingsTestConstants;

import java.util.Arrays;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/10/17.
 */
public class InfrastructureMappingServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Inject @InjectMocks private InfrastructureMappingService infrastructureMappingService;

  private SettingAttribute computeProviderSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig().build())
          .build();

  @Before
  public void setUp() throws Exception {}

  @Test
  public void shouldList() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withAccessType(AccessType.USER_PASSWORD)
                                                                      .withConnectionType(ConnectionType.SSH)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .build();

    PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
    PageResponse pageResponse = aPageResponse().withResponse(Arrays.asList(physicalInfrastructureMapping)).build();
    when(wingsPersistence.query(InfrastructureMapping.class, pageRequest)).thenReturn(pageResponse);

    PageResponse<InfrastructureMapping> infrastructureMappings = infrastructureMappingService.list(pageRequest);
    assertThat(infrastructureMappings).hasSize(1).containsExactly(physicalInfrastructureMapping);
    verify(wingsPersistence).query(InfrastructureMapping.class, pageRequest);
  }

  @Test
  public void shouldSave() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withAccessType(AccessType.USER_PASSWORD)
                                                                      .withConnectionType(ConnectionType.SSH)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .build();
    PhysicalInfrastructureMapping savedPhysicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withAccessType(AccessType.USER_PASSWORD)
            .withConnectionType(ConnectionType.SSH)
            .withComputeProviderSettingId(SETTING_ID)
            .withUuid(WingsTestConstants.INFRA_MAPPING_ID)
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withServiceTemplateId(TEMPLATE_ID)
            .build();

    when(wingsPersistence.saveAndGet(InfrastructureMapping.class, physicalInfrastructureMapping))
        .thenReturn(savedPhysicalInfrastructureMapping);

    InfrastructureMapping returnedInfrastructureMapping =
        infrastructureMappingService.save(physicalInfrastructureMapping);

    assertThat(returnedInfrastructureMapping.getUuid()).isEqualTo(INFRA_MAPPING_ID);
    verify(wingsPersistence).saveAndGet(InfrastructureMapping.class, physicalInfrastructureMapping);
  }

  @Test
  public void shouldGet() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withAccessType(AccessType.USER_PASSWORD)
                                                                      .withConnectionType(ConnectionType.SSH)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withUuid(INFRA_MAPPING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .build();

    when(wingsPersistence.get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID))
        .thenReturn(physicalInfrastructureMapping);

    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(APP_ID, ENV_ID, INFRA_MAPPING_ID);
    assertThat(infrastructureMapping.getUuid()).isEqualTo(INFRA_MAPPING_ID);
    verify(wingsPersistence).get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);
  }

  @Test
  @Ignore
  public void shouldUpdate() {}

  @Test
  public void shouldDelete() {
    infrastructureMappingService.delete(APP_ID, ENV_ID, INFRA_MAPPING_ID);
    verify(wingsPersistence).delete(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);
  }
}
