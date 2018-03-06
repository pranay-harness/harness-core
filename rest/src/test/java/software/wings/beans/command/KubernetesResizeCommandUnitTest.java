package software.wings.beans.command;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.KubernetesResizeParams.KubernetesResizeParamsBuilder.aKubernetesResizeParams;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerServiceData;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.KubernetesResizeParams.KubernetesResizeParamsBuilder;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.Status;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public class KubernetesResizeCommandUnitTest extends WingsBaseTest {
  @Mock private GkeClusterService gkeClusterService;
  @Mock private KubernetesContainerService kubernetesContainerService;

  @InjectMocks private KubernetesResizeCommandUnit kubernetesResizeCommandUnit = new KubernetesResizeCommandUnit();

  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                                  .masterUrl("https://1.1.1.1/")
                                                  .username("admin")
                                                  .password("password".toCharArray())
                                                  .namespace("default")
                                                  .build();
  private KubernetesResizeParamsBuilder resizeParamsBuilder = aKubernetesResizeParams()
                                                                  .withClusterName(CLUSTER_NAME)
                                                                  .withNamespace("default")
                                                                  .withUseAutoscaler(false)
                                                                  .withSubscriptionId("subscriptionId")
                                                                  .withResourceGroup("resourceGroup")
                                                                  .withApiVersion("v1")
                                                                  .withUseIstioRouteRule(false)
                                                                  .withRollback(false)
                                                                  .withResizeStrategy(ResizeStrategy.RESIZE_NEW_FIRST);
  private SettingAttribute computeProvider = aSettingAttribute().withValue(GcpConfig.builder().build()).build();
  private CommandExecutionContext.Builder contextBuilder =
      aCommandExecutionContext().withCloudProviderSetting(computeProvider).withCloudProviderCredentials(emptyList());

  @Before
  public void setup() {
    when(gkeClusterService.getCluster(any(SettingAttribute.class), eq(emptyList()), anyString(), anyString()))
        .thenReturn(kubernetesConfig);
    when(kubernetesContainerService.setControllerPodCount(
             eq(kubernetesConfig), any(), eq(CLUSTER_NAME), anyString(), anyInt(), anyInt(), anyInt(), any()))
        .thenAnswer(i -> buildContainerInfos((Integer) i.getArguments()[5]));
  }

  private List<ContainerInfo> buildContainerInfos(int count) {
    List<ContainerInfo> containerInfos = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      containerInfos.add(ContainerInfo.builder().status(Status.SUCCESS).build());
    }
    return containerInfos;
  }

  @Test
  public void shouldExecuteThrowInvalidRequest() {
    when(kubernetesContainerService.getControllerPodCount(eq(kubernetesConfig), any(), anyString()))
        .thenReturn(Optional.empty());
    try {
      KubernetesResizeParams resizeParams = resizeParamsBuilder.but()
                                                .withContainerServiceName("rc-name.0")
                                                .withUseFixedInstances(false)
                                                .withMaxInstances(5)
                                                .withInstanceCount(100)
                                                .withInstanceUnitType(PERCENTAGE)
                                                .build();

      CommandExecutionContext context = contextBuilder.but().withContainerResizeParams(resizeParams).build();
      kubernetesResizeCommandUnit.execute(context);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1).containsKey("message");
      assertThat(exception.getParams().get("message")).asString().contains("Service setup not done, service name:");
    }
  }

  @Test
  public void shouldUseMaxInstancesWithPercentage() {
    LinkedHashMap<String, Integer> activeServiceCounts = new LinkedHashMap<>();

    ResizeCommandUnitExecutionData executionData =
        execute(activeServiceCounts, 0, "rc-name.0", false, 5, 0, 100, PERCENTAGE);

    assertThat(executionData.getContainerInfos().size()).isEqualTo(5);
    assertThat(executionData.getNewInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextNewServiceData = executionData.getNewInstanceData().get(0);
    assertThat(contextNewServiceData.getPreviousCount()).isEqualTo(0);
    assertThat(contextNewServiceData.getDesiredCount()).isEqualTo(5);

    assertThat(executionData.getOldInstanceData().size()).isEqualTo(0);
  }

  @Test
  public void shouldUseMaxInstancesWithAnyPercentage() {
    LinkedHashMap<String, Integer> activeServiceCounts = new LinkedHashMap<>();

    ResizeCommandUnitExecutionData executionData =
        execute(activeServiceCounts, 0, "rc-name.0", false, 5, 0, 20, PERCENTAGE);

    assertThat(executionData.getContainerInfos().size()).isEqualTo(5);
    assertThat(executionData.getNewInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextNewServiceData = executionData.getNewInstanceData().get(0);
    assertThat(contextNewServiceData.getPreviousCount()).isEqualTo(0);
    assertThat(contextNewServiceData.getDesiredCount()).isEqualTo(5);

    assertThat(executionData.getOldInstanceData().size()).isEqualTo(0);
  }

  @Test
  public void shouldResizeAndDownsize() {
    LinkedHashMap<String, Integer> activeServiceCounts = new LinkedHashMap<>();
    activeServiceCounts.put("rc-name.0", 1);

    ResizeCommandUnitExecutionData executionData = execute(activeServiceCounts, 1, "rc-name.1", false, 0, 0, 2, COUNT);

    assertThat(executionData.getContainerInfos().size()).isEqualTo(2);
    assertThat(executionData.getNewInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextNewServiceData = executionData.getNewInstanceData().get(0);
    assertThat(contextNewServiceData.getPreviousCount()).isEqualTo(1);
    assertThat(contextNewServiceData.getDesiredCount()).isEqualTo(2);

    assertThat(executionData.getOldInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextOldServiceData = executionData.getOldInstanceData().get(0);
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(1);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
  }

  @Test
  public void shouldDownsizeMultiple() {
    LinkedHashMap<String, Integer> activeServiceCounts = new LinkedHashMap<>();
    activeServiceCounts.put("rc-name.0", 1);
    activeServiceCounts.put("rc-name.1", 2);

    ResizeCommandUnitExecutionData executionData = execute(activeServiceCounts, 0, "rc-name.2", false, 0, 0, 3, COUNT);

    assertThat(executionData.getContainerInfos().size()).isEqualTo(3);
    assertThat(executionData.getNewInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextNewServiceData = executionData.getNewInstanceData().get(0);
    assertThat(contextNewServiceData.getPreviousCount()).isEqualTo(0);
    assertThat(contextNewServiceData.getDesiredCount()).isEqualTo(3);

    assertThat(executionData.getOldInstanceData().size()).isEqualTo(2);
    ContainerServiceData contextOldServiceData = executionData.getOldInstanceData().get(0);
    assertThat(contextOldServiceData.getName()).isEqualTo("rc-name.0");
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(1);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
    contextOldServiceData = executionData.getOldInstanceData().get(1);
    assertThat(contextOldServiceData.getName()).isEqualTo("rc-name.1");
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(2);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
  }

  @Test
  public void shouldUseFixedInstancesWithCount() {
    LinkedHashMap<String, Integer> activeServiceCounts = new LinkedHashMap<>();
    activeServiceCounts.put("rc-name.0", 2);
    activeServiceCounts.put("rc-name.1", 2);

    ResizeCommandUnitExecutionData executionData = execute(activeServiceCounts, 0, "rc-name.2", true, 0, 3, 3, COUNT);

    assertThat(executionData.getContainerInfos().size()).isEqualTo(3);
    assertThat(executionData.getNewInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextNewServiceData = executionData.getNewInstanceData().get(0);
    assertThat(contextNewServiceData.getPreviousCount()).isEqualTo(0);
    assertThat(contextNewServiceData.getDesiredCount()).isEqualTo(3);

    assertThat(executionData.getOldInstanceData().size()).isEqualTo(2);
    ContainerServiceData contextOldServiceData = executionData.getOldInstanceData().get(0);
    assertThat(contextOldServiceData.getName()).isEqualTo("rc-name.0");
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(2);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
    contextOldServiceData = executionData.getOldInstanceData().get(1);
    assertThat(contextOldServiceData.getName()).isEqualTo("rc-name.1");
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(2);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
  }

  @Test
  public void shouldCapCountAtFixed() {
    LinkedHashMap<String, Integer> activeServiceCounts = new LinkedHashMap<>();
    activeServiceCounts.put("rc-name.0", 3);

    ResizeCommandUnitExecutionData executionData = execute(activeServiceCounts, 0, "rc-name.1", true, 0, 3, 5, COUNT);

    assertThat(executionData.getContainerInfos().size()).isEqualTo(3);
    assertThat(executionData.getNewInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextNewServiceData = executionData.getNewInstanceData().get(0);
    assertThat(contextNewServiceData.getPreviousCount()).isEqualTo(0);
    assertThat(contextNewServiceData.getDesiredCount()).isEqualTo(3);

    assertThat(executionData.getOldInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextOldServiceData = executionData.getOldInstanceData().get(0);
    assertThat(contextOldServiceData.getName()).isEqualTo("rc-name.0");
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(3);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
  }

  @Test
  public void shouldUseFixedInstancesWithPercentage() {
    LinkedHashMap<String, Integer> activeServiceCounts = new LinkedHashMap<>();
    activeServiceCounts.put("rc-name.0", 2);
    activeServiceCounts.put("rc-name.1", 2);

    ResizeCommandUnitExecutionData executionData =
        execute(activeServiceCounts, 0, "rc-name.2", true, 0, 3, 100, PERCENTAGE);

    assertThat(executionData.getContainerInfos().size()).isEqualTo(3);
    assertThat(executionData.getNewInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextNewServiceData = executionData.getNewInstanceData().get(0);
    assertThat(contextNewServiceData.getPreviousCount()).isEqualTo(0);
    assertThat(contextNewServiceData.getDesiredCount()).isEqualTo(3);

    assertThat(executionData.getOldInstanceData().size()).isEqualTo(2);
    ContainerServiceData contextOldServiceData = executionData.getOldInstanceData().get(0);
    assertThat(contextOldServiceData.getName()).isEqualTo("rc-name.0");
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(2);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
    contextOldServiceData = executionData.getOldInstanceData().get(1);
    assertThat(contextOldServiceData.getName()).isEqualTo("rc-name.1");
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(2);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
  }

  @Test
  public void shouldNotUseMaxInstancesWhenAlreadyRunningWithPercentage() {
    LinkedHashMap<String, Integer> activeServiceCounts = new LinkedHashMap<>();
    activeServiceCounts.put("rc-name.0", 10);

    ResizeCommandUnitExecutionData executionData =
        execute(activeServiceCounts, 0, "rc-name.1", false, 5, 0, 100, PERCENTAGE);

    assertThat(executionData.getContainerInfos().size()).isEqualTo(10);
    assertThat(executionData.getNewInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextNewServiceData = executionData.getNewInstanceData().get(0);
    assertThat(contextNewServiceData.getPreviousCount()).isEqualTo(0);
    assertThat(contextNewServiceData.getDesiredCount()).isEqualTo(10);

    assertThat(executionData.getOldInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextOldServiceData = executionData.getOldInstanceData().get(0);
    assertThat(contextOldServiceData.getName()).isEqualTo("rc-name.0");
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(10);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
  }

  @Test
  public void shouldNotUseMaxInstancesWhenAlreadyRunningLessThanMaxWithPercentage() {
    LinkedHashMap<String, Integer> activeServiceCounts = new LinkedHashMap<>();
    activeServiceCounts.put("rc-name.0", 3);

    ResizeCommandUnitExecutionData executionData =
        execute(activeServiceCounts, 0, "rc-name.1", false, 5, 0, 100, PERCENTAGE);

    assertThat(executionData.getContainerInfos().size()).isEqualTo(3);
    assertThat(executionData.getNewInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextNewServiceData = executionData.getNewInstanceData().get(0);
    assertThat(contextNewServiceData.getPreviousCount()).isEqualTo(0);
    assertThat(contextNewServiceData.getDesiredCount()).isEqualTo(3);

    assertThat(executionData.getOldInstanceData().size()).isEqualTo(1);
    ContainerServiceData contextOldServiceData = executionData.getOldInstanceData().get(0);
    assertThat(contextOldServiceData.getName()).isEqualTo("rc-name.0");
    assertThat(contextOldServiceData.getPreviousCount()).isEqualTo(3);
    assertThat(contextOldServiceData.getDesiredCount()).isEqualTo(0);
  }

  private ResizeCommandUnitExecutionData execute(LinkedHashMap<String, Integer> activeServiceCounts,
      int controllerPodCount, String controllerName, boolean useFixedInstances, int maxInstances, int fixedInstances,
      int instanceCount, InstanceUnitType instanceUnitType) {
    when(kubernetesContainerService.getControllerPodCount(eq(kubernetesConfig), any(), anyString()))
        .thenReturn(Optional.of(controllerPodCount));
    when(kubernetesContainerService.getActiveServiceCounts(eq(kubernetesConfig), any(), anyString()))
        .thenReturn(activeServiceCounts);
    KubernetesResizeParams resizeParams = resizeParamsBuilder.but()
                                              .withContainerServiceName(controllerName)
                                              .withUseFixedInstances(useFixedInstances)
                                              .withInstanceCount(instanceCount)
                                              .withInstanceUnitType(instanceUnitType)
                                              .withFixedInstances(fixedInstances)
                                              .withMaxInstances(maxInstances)
                                              .build();

    CommandExecutionContext context = contextBuilder.but().withContainerResizeParams(resizeParams).build();
    CommandExecutionStatus status = kubernetesResizeCommandUnit.execute(context);
    ResizeCommandUnitExecutionData executionData = (ResizeCommandUnitExecutionData) context.getCommandExecutionData();

    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
    return executionData;
  }
}
