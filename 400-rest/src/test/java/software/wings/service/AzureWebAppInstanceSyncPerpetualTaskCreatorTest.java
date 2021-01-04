package software.wings.service;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AzureWebAppInstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.AzureWebAppDeploymentKey;
import software.wings.service.intfc.instance.InstanceService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class AzureWebAppInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock private InstanceService mockInstanceService;
  @Mock private PerpetualTaskService mockPerpetualTaskService;

  @InjectMocks @Inject AzureWebAppInstanceSyncPerpetualTaskCreator creator;

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testCreatePerpetualTasks() {
    doReturn(singletonList(Instance.builder()
                               .instanceInfo(AzureWebAppInstanceInfo.builder()
                                                 .instanceId("instanceId")
                                                 .instanceType("instanceType")
                                                 .appName("appName")
                                                 .appServicePlanId("appServicePlanId")
                                                 .host("host")
                                                 .slotId("slotId")
                                                 .slotName("slotName")
                                                 .state("running")
                                                 .build())
                               .build()))
        .when(mockInstanceService)
        .getInstancesForAppAndInframapping(anyString(), anyString());
    doReturn("taskId")
        .when(mockPerpetualTaskService)
        .createTask(anyString(), anyString(), any(), any(), anyBoolean(), anyString());
    ArgumentCaptor<PerpetualTaskClientContext> clientContextCaptor =
        ArgumentCaptor.forClass(PerpetualTaskClientContext.class);
    AzureWebAppInfrastructureMapping infrastructureMapping = buildAzureWebAppInfrastructureMapping();

    creator.createPerpetualTasks(infrastructureMapping);

    verify(mockPerpetualTaskService)
        .createTask(anyString(), anyString(), clientContextCaptor.capture(), any(), anyBoolean(), anyString());
    PerpetualTaskClientContext context = clientContextCaptor.getValue();
    assertThat(context).isNotNull();
    Map<String, String> clientParams = context.getClientParams();
    assertThat(clientParams).isNotNull();
    assertThat(clientParams.get("appName")).isEqualTo("appName");
    assertThat(clientParams.get("slotName")).isEqualTo("slotName");
  }

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testCreatePerpetualTasksForNewDeployment() {
    List<DeploymentSummary> deploymentSummaries =
        singletonList(DeploymentSummary.builder()
                          .azureWebAppDeploymentKey(
                              AzureWebAppDeploymentKey.builder().appName("newAppName").slotName("newSlotName").build())
                          .build());
    List<PerpetualTaskRecord> existingPerpetualTasks = singletonList(
        PerpetualTaskRecord.builder()
            .clientContext(PerpetualTaskClientContext.builder()
                               .clientParams(ImmutableMap.of("appName", "oldAppName", "slotName", "oldSlotName"))
                               .build())
            .build());
    AzureWebAppInfrastructureMapping infrastructureMapping = buildAzureWebAppInfrastructureMapping();
    ArgumentCaptor<PerpetualTaskClientContext> clientContextCaptor =
        ArgumentCaptor.forClass(PerpetualTaskClientContext.class);

    creator.createPerpetualTasksForNewDeployment(deploymentSummaries, existingPerpetualTasks, infrastructureMapping);

    verify(mockPerpetualTaskService)
        .createTask(anyString(), anyString(), clientContextCaptor.capture(), any(), anyBoolean(), anyString());
    PerpetualTaskClientContext context = clientContextCaptor.getValue();
    assertThat(context).isNotNull();
    Map<String, String> clientParams = context.getClientParams();
    assertThat(clientParams).isNotNull();
    assertThat(clientParams.get("appName")).isEqualTo("newAppName");
    assertThat(clientParams.get("slotName")).isEqualTo("newSlotName");
  }

  @NotNull
  private AzureWebAppInfrastructureMapping buildAzureWebAppInfrastructureMapping() {
    AzureWebAppInfrastructureMapping infrastructureMapping = AzureWebAppInfrastructureMapping.builder().build();
    infrastructureMapping.setAppId(APP_ID);
    infrastructureMapping.setAccountId(ACCOUNT_ID);
    infrastructureMapping.setUuid(INFRA_MAPPING_ID);
    return infrastructureMapping;
  }
}
