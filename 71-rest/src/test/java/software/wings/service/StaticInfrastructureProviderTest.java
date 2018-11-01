package software.wings.service;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.StaticInfrastructureProvider;
import software.wings.service.intfc.HostService;

import java.util.Collections;

/**
 * Created by anubhaw on 1/24/17.
 */

public class StaticInfrastructureProviderTest extends WingsBaseTest {
  @Mock private HostService hostService;

  @Inject @InjectMocks private StaticInfrastructureProvider infrastructureProvider = new StaticInfrastructureProvider();

  @Test
  public void shouldListHosts() {
    Host host = aHost().withHostName(HOST_NAME).build();
    when(hostService.list(any(PageRequest.class))).thenReturn(aPageResponse().withResponse(asList(host)).build());
    SettingAttribute computeProviderSettingAttribute =
        aSettingAttribute().withUuid(COMPUTE_PROVIDER_ID).withValue(aPhysicalDataCenterConfig().build()).build();
    PageResponse<Host> hosts = infrastructureProvider.listHosts(
        null, computeProviderSettingAttribute, Collections.emptyList(), new PageRequest<>());
    assertThat(hosts).hasSize(1).containsExactly(host);
    verify(hostService).list(any(PageRequest.class));
  }

  @Test
  public void shouldSaveHost() {
    Host reqHost = aHost().withHostName(HOST_NAME).build();
    Host savedHost = aHost().withUuid(HOST_ID).withHostName(HOST_NAME).build();

    when(hostService.saveHost(reqHost)).thenReturn(savedHost);

    Host host = infrastructureProvider.saveHost(reqHost);
    assertThat(host).isNotNull().isEqualTo(savedHost);
    verify(hostService).saveHost(reqHost);
  }

  @Test
  public void shouldDeleteHost() {
    infrastructureProvider.deleteHost(APP_ID, INFRA_MAPPING_ID, HOST_NAME);
    verify(hostService).deleteByDnsName(APP_ID, INFRA_MAPPING_ID, HOST_NAME);
  }

  @Test
  public void shouldUpdateHostConnAttrs() {
    infrastructureProvider.updateHostConnAttrs(aPhysicalInfrastructureMapping()
                                                   .withAppId(APP_ID)
                                                   .withUuid(INFRA_MAPPING_ID)
                                                   .withDeploymentType(DeploymentType.WINRM.toString())
                                                   .build(),
        HOST_CONN_ATTR_ID);
    verify(hostService)
        .updateHostConnectionAttrByInfraMappingId(
            APP_ID, INFRA_MAPPING_ID, HOST_CONN_ATTR_ID, DeploymentType.WINRM.toString());
  }
}
