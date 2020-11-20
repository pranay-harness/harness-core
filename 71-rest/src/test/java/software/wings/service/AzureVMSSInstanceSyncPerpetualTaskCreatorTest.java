package software.wings.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AzureVMSSInstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.AzureVMSSDeploymentKey;
import software.wings.service.intfc.instance.InstanceService;

import java.util.List;
import java.util.Map;

public class AzureVMSSInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock private InstanceService mockInstanceService;
  @Mock private PerpetualTaskService mockPerpetualTaskService;

  @InjectMocks @Inject AzureVMSSInstanceSyncPerpetualTaskCreator creator;

  @Test
  @Owner(developers = OwnerRule.SATYAM)
  @Category(UnitTests.class)
  public void testCreatePerpetualTasks() {
    doReturn(singletonList(Instance.builder()
                               .instanceInfo(AzureVMSSInstanceInfo.builder()
                                                 .vmssId("vmss-id")
                                                 .azureVMId("azure-vm-id")
                                                 .instanceType("instance-type")
                                                 .host("host-ip")
                                                 .state("state")
                                                 .build())
                               .build()))
        .when(mockInstanceService)
        .getInstancesForAppAndInframapping(anyString(), anyString());
    doReturn("perp-tesk-id")
        .when(mockPerpetualTaskService)
        .createTask(anyString(), anyString(), any(), any(), anyBoolean(), anyString());
    ArgumentCaptor<PerpetualTaskClientContext> clientContextCaptor =
        ArgumentCaptor.forClass(PerpetualTaskClientContext.class);
    AzureVMSSInfrastructureMapping infrastructureMapping = AzureVMSSInfrastructureMapping.builder().build();
    infrastructureMapping.setAppId(APP_ID);
    infrastructureMapping.setAccountId(ACCOUNT_ID);
    infrastructureMapping.setUuid(INFRA_MAPPING_ID);
    creator.createPerpetualTasks(infrastructureMapping);
    verify(mockPerpetualTaskService)
        .createTask(anyString(), anyString(), clientContextCaptor.capture(), any(), anyBoolean(), anyString());
    PerpetualTaskClientContext context = clientContextCaptor.getValue();
    assertThat(context).isNotNull();
    Map<String, String> clientParams = context.getClientParams();
    assertThat(clientParams).isNotNull();
    assertThat(clientParams.get("vmssId")).isEqualTo("vmss-id");
  }

  @Test
  @Owner(developers = OwnerRule.SATYAM)
  @Category(UnitTests.class)
  public void testCreatePerpetualTasksForNewDeployment() {
    List<DeploymentSummary> deploymentSummaries =
        singletonList(DeploymentSummary.builder()
                          .azureVMSSDeploymentKey(AzureVMSSDeploymentKey.builder().vmssId("vmss-id-new").build())
                          .build());
    List<PerpetualTaskRecord> existingPerpetualTasks = singletonList(
        PerpetualTaskRecord.builder()
            .clientContext(
                PerpetualTaskClientContext.builder().clientParams(ImmutableMap.of("vmssId", "vmss-id-old")).build())
            .build());
    AzureVMSSInfrastructureMapping infrastructureMapping = AzureVMSSInfrastructureMapping.builder().build();
    infrastructureMapping.setAppId(APP_ID);
    infrastructureMapping.setAccountId(ACCOUNT_ID);
    infrastructureMapping.setUuid(INFRA_MAPPING_ID);
    ArgumentCaptor<PerpetualTaskClientContext> clientContextCaptor =
        ArgumentCaptor.forClass(PerpetualTaskClientContext.class);
    creator.createPerpetualTasksForNewDeployment(deploymentSummaries, existingPerpetualTasks, infrastructureMapping);
    verify(mockPerpetualTaskService)
        .createTask(anyString(), anyString(), clientContextCaptor.capture(), any(), anyBoolean(), anyString());
    PerpetualTaskClientContext context = clientContextCaptor.getValue();
    assertThat(context).isNotNull();
    Map<String, String> clientParams = context.getClientParams();
    assertThat(clientParams).isNotNull();
    assertThat(clientParams.get("vmssId")).isEqualTo("vmss-id-new");
  }
}
