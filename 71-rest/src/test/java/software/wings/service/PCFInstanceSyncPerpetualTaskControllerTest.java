package software.wings.service;

import static io.harness.rule.OwnerRule.AMAN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.instanceSync.PcfInstanceSyncPerpTaskClient;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.api.PcfDeploymentInfo;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.instance.InstanceService;

import java.util.Arrays;

public class PCFInstanceSyncPerpetualTaskControllerTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "accountId";
  @Mock FeatureFlagService featureFlagService;
  @Mock AppService appService;
  @Mock InstanceService instanceService;
  @Mock InfrastructureMappingService infrastructureMappingService;
  @Mock PcfInstanceSyncPerpTaskClient pcfPerpTaskClient;

  @InjectMocks PCFInstanceSyncPerpetualTaskController pcfInstanceSyncPerpetualTaskController;

  @Before
  public void setUp() throws Exception {
    doNothing().when(featureFlagService).enableAccount(any(), any());
    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(Arrays.asList("appId"));
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(true);

    PcfInfrastructureMapping pcfInfrastructureMapping =
        PcfInfrastructureMapping.builder().accountId(ACCOUNT_ID).infraMappingType("PCF_PCF").build();

    PageResponse<InfrastructureMapping> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(pcfInfrastructureMapping));
    when(infrastructureMappingService.list(any())).thenReturn(pageResponse);
    when(pcfPerpTaskClient.create(any(), any())).thenReturn("success");
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void enablePerpetualTaskForAccount() {
    boolean enablePerpetualTaskForAccount =
        pcfInstanceSyncPerpetualTaskController.enablePerpetualTaskForAccount(ACCOUNT_ID);
    assertThat(enablePerpetualTaskForAccount).isTrue();
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void createPerpetualTaskForNewDeployment() {
    DeploymentSummary deploymentSummary =
        DeploymentSummary.builder()
            .appId("appId")
            .accountId("accountId")
            .infraMappingId("infraId")
            .deploymentInfo(
                PcfDeploymentInfo.builder().applicationGuild("guid").applicationName("applicationName").build())
            .build();
    pcfInstanceSyncPerpetualTaskController.createPerpetualTaskForNewDeployment(
        InfrastructureMappingType.PCF_PCF, Arrays.asList(deploymentSummary));
  }
}