package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder.aPhaseExecutionData;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.ContainerInstanceHandler;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by sriram_parthasarathy on 12/7/17.
 */
public class AbstractAnalysisStateTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Mock private StateExecutionService stateExecutionService;
  @Mock private ContainerInstanceHandler containerInstanceHandler;
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private InfrastructureMapping infrastructureMapping;
  @Mock private ServiceResourceService serviceResourceService;

  private final String workflowId = UUID.randomUUID().toString();
  private final String envId = UUID.randomUUID().toString();
  private final String appId = UUID.randomUUID().toString();
  private final String serviceId = UUID.randomUUID().toString();
  private final String previousWorkflowExecutionId = UUID.randomUUID().toString();
  private final String infraMappingId = generateUuid();

  public static final String PHASE_PARAM = "PHASE_PARAM";
  @Before
  public void setup() {
    initMocks(this);
    when(containerInstanceHandler.isContainerDeployment(anyObject())).thenReturn(false);
    when(infrastructureMapping.getDeploymentType()).thenReturn(DeploymentType.KUBERNETES.name());
    when(infraMappingService.get(anyString(), anyString())).thenReturn(infrastructureMapping);
    when(serviceResourceService.getDeploymentType(any(), any(), any())).thenReturn(DeploymentType.KUBERNETES);
    when(serviceResourceService.get(anyString(), anyString(), anyBoolean()))
        .thenReturn(Service.builder().uuid(serviceId).name("ServiceA").build());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastExecutionNodes()
      throws NoSuchAlgorithmException, KeyManagementException, IllegalAccessException {
    List<ElementExecutionSummary> elementExecutionSummary = new ArrayList<>();
    for (String service : new String[] {"serviceA", "serviceB"}) {
      List<InstanceStatusSummary> instanceStatusSummaryList = new ArrayList<>();
      for (int i = 0; i < 5; ++i) {
        instanceStatusSummaryList.add(
            anInstanceStatusSummary()
                .withInstanceElement(anInstanceElement().hostName(service + "-" + i + ".harness.com").build())
                .build());
      }
      elementExecutionSummary.add(anElementExecutionSummary()
                                      .withContextElement(ServiceElement.builder().uuid(service).build())
                                      .withInstanceStatusSummaries(instanceStatusSummaryList)
                                      .build());
    }
    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .appId(appId)
            .uuid(previousWorkflowExecutionId)
            .workflowId(workflowId)
            .envId(envId)
            .serviceIds(Arrays.asList(serviceId))
            .infraMappingIds(Arrays.asList(infraMappingId))
            .status(ExecutionStatus.SUCCESS)
            .serviceExecutionSummaries(elementExecutionSummary)
            .breakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();

    Workflow workflow = WorkflowBuilder.aWorkflow().appId(appId).uuid(workflowId).envId(envId).build();

    wingsPersistence.save(workflow);
    wingsPersistence.save(workflowExecution);

    ExecutionContext context = spy(new ExecutionContextImpl(new StateExecutionInstance()));
    doReturn(PhaseElement.builder()
                 .infraMappingId(infraMappingId)
                 .serviceElement(ServiceElement.builder().uuid("serviceA").build())
                 .build())
        .when(context)
        .getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    WorkflowStandardParams wsp = WorkflowStandardParams.Builder.aWorkflowStandardParams().build();
    Reflect.on(wsp).set("env", Environment.Builder.anEnvironment().uuid(envId).build());
    doReturn(wsp).when(context).getContextElement(ContextElementType.STANDARD);
    doReturn(appId).when(context).getAppId();
    doReturn(infraMappingId).when(context).fetchInfraMappingId();
    doReturn(UUID.randomUUID().toString()).when(context).getWorkflowExecutionId();

    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));
    doReturn(workflowId).when(splunkV2State).getWorkflowId(context);

    FieldUtils.writeField(splunkV2State, "containerInstanceHandler", containerInstanceHandler, true);
    FieldUtils.writeField(splunkV2State, "infraMappingService", infraMappingService, true);
    FieldUtils.writeField(splunkV2State, "serviceResourceService", serviceResourceService, true);
    Reflect.on(splunkV2State).set("workflowExecutionService", workflowExecutionService);

    Reflect.on(splunkV2State).set("stateExecutionService", stateExecutionService);

    Map<String, String> nodes = splunkV2State.getLastExecutionNodes(context);
    assertThat(nodes).hasSize(5);
    for (int i = 0; i < 5; ++i) {
      final String serviceNAme = "serviceA-" + i + ".harness.com";
      assertThat(nodes.keySet().contains(serviceNAme)).isTrue();
      assertThat(nodes.get(serviceNAme)).isEqualTo(DEFAULT_GROUP_NAME);
      nodes.remove(serviceNAme);
      assertThat(nodes.keySet().contains("serviceA-" + i)).isFalse();
      nodes.remove("serviceA-" + i);
    }
    assertThat(nodes).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLastExecutionNodesWithPhase()
      throws NoSuchAlgorithmException, KeyManagementException, IllegalAccessException {
    List<ElementExecutionSummary> elementExecutionSummary = new ArrayList<>();
    for (String service : new String[] {"serviceA", "serviceB"}) {
      List<InstanceStatusSummary> instanceStatusSummaryList = new ArrayList<>();
      for (int i = 0; i < 5; ++i) {
        instanceStatusSummaryList.add(
            anInstanceStatusSummary()
                .withInstanceElement(anInstanceElement().hostName(service + "-" + i + ".harness.com").build())
                .build());
      }
      elementExecutionSummary.add(anElementExecutionSummary()
                                      .withContextElement(ServiceElement.builder().uuid(service).build())
                                      .withInstanceStatusSummaries(instanceStatusSummaryList)
                                      .build());
    }
    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .appId(appId)
            .uuid(previousWorkflowExecutionId)
            .workflowId(workflowId)
            .envId(envId)
            .serviceIds(Arrays.asList(serviceId))
            .infraMappingIds(Arrays.asList(infraMappingId))
            .status(ExecutionStatus.SUCCESS)
            .serviceExecutionSummaries(elementExecutionSummary)
            .breakdown(CountsByStatuses.Builder.aCountsByStatuses().withSuccess(1).build())
            .build();

    Workflow workflow = WorkflowBuilder.aWorkflow().appId(appId).uuid(workflowId).envId(envId).build();

    wingsPersistence.save(workflow);
    wingsPersistence.save(workflowExecution);

    StateExecutionInstance stateExecutionInstance = mock(StateExecutionInstance.class);

    Map<String, StateExecutionData> stateExecutionDataMap = new HashMap<>();
    StateExecutionData stateExecutionData =
        aPhaseExecutionData()
            .withElementStatusSummary(Lists.newArrayList(
                anElementExecutionSummary()
                    .withContextElement(PhaseElement.builder()
                                            .serviceElement(ServiceElement.builder().uuid("serviceA").build())
                                            .build())
                    .withInstanceStatusSummaries(Lists.newArrayList(
                        anInstanceStatusSummary()
                            .withInstanceElement(anInstanceElement().hostName("serviceA-0.harness.com").build())
                            .build(),
                        anInstanceStatusSummary()
                            .withInstanceElement(anInstanceElement().hostName("serviceA-1.harness.com").build())
                            .build()))
                    .build()))
            .build();
    stateExecutionData.setStateType(StateType.PHASE.name());
    stateExecutionDataMap.put(UUID.randomUUID().toString(), stateExecutionData);

    ExecutionContext context = spy(new ExecutionContextImpl(stateExecutionInstance));
    when(stateExecutionInstance.getStateExecutionMap()).thenReturn(stateExecutionDataMap);

    doReturn(PhaseElement.builder()
                 .infraMappingId(infraMappingId)
                 .serviceElement(ServiceElement.builder().uuid("serviceA").build())
                 .build())
        .when(context)
        .getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    doReturn(appId).when(context).getAppId();
    doReturn(infraMappingId).when(context).fetchInfraMappingId();
    doReturn(UUID.randomUUID().toString()).when(context).getWorkflowExecutionId();

    WorkflowStandardParams wsp = WorkflowStandardParams.Builder.aWorkflowStandardParams().build();
    Reflect.on(wsp).set("env", Environment.Builder.anEnvironment().uuid(envId).build());
    doReturn(wsp).when(context).getContextElement(ContextElementType.STANDARD);

    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));
    doReturn(workflowId).when(splunkV2State).getWorkflowId(context);

    FieldUtils.writeField(splunkV2State, "containerInstanceHandler", containerInstanceHandler, true);
    FieldUtils.writeField(splunkV2State, "infraMappingService", infraMappingService, true);
    FieldUtils.writeField(splunkV2State, "serviceResourceService", serviceResourceService, true);
    Reflect.on(splunkV2State).set("workflowExecutionService", workflowExecutionService);
    Reflect.on(splunkV2State).set("stateExecutionService", stateExecutionService);

    when(stateExecutionService.fetchPhaseExecutionData(anyString(), anyString(), anyString(), any()))
        .thenReturn(Arrays.asList(stateExecutionData));

    Map<String, String> nodes = splunkV2State.getLastExecutionNodes(context);
    assertThat(nodes).hasSize(3);
    assertThat(nodes.keySet().contains("serviceA-0.harness.com")).isFalse();
    assertThat(nodes.keySet().contains("serviceA-1.harness.com")).isFalse();
    for (int i = 2; i < 5; ++i) {
      assertThat(nodes.keySet().contains("serviceA-" + i + ".harness.com")).isTrue();
      assertThat(nodes.get("serviceA-" + i + ".harness.com")).isEqualTo(DEFAULT_GROUP_NAME);
      nodes.remove("serviceA-" + i + ".harness.com");
      assertThat(nodes.keySet().contains("serviceA-" + i)).isFalse();
      nodes.remove("serviceA-" + i);
    }
    assertThat(nodes).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCanaryNewNodes() throws NoSuchAlgorithmException, KeyManagementException, IllegalAccessException {
    List<InstanceElement> instanceElements = new ArrayList<>();
    for (int i = 0; i < 5; ++i) {
      instanceElements.add(
          anInstanceElement().hostName("serviceA-" + i + ".harness.com").workloadName("workload-" + i).build());
    }
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    CanaryWorkflowStandardParams params = Mockito.mock(CanaryWorkflowStandardParams.class);
    doReturn(instanceElements).when(params).getInstances();
    when(context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM))
        .thenReturn(PhaseElement.builder().infraMappingId(UUID.randomUUID().toString()).build());
    when(context.getAppId()).thenReturn(appId);
    when(context.fetchInfraMappingId()).thenReturn(infraMappingId);

    doReturn(params).when(context).getContextElement(ContextElementType.STANDARD);
    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));

    FieldUtils.writeField(splunkV2State, "infraMappingService", infraMappingService, true);
    FieldUtils.writeField(splunkV2State, "serviceResourceService", serviceResourceService, true);
    Map<String, String> nodes = splunkV2State.getCanaryNewHostNames(context);
    assertThat(nodes).hasSize(5);
    for (int i = 0; i < 5; ++i) {
      assertThat(nodes.keySet().contains("serviceA"
                     + "-" + i + ".harness.com"))
          .isTrue();
      assertThat(nodes.get("serviceA"
                     + "-" + i + ".harness.com"))
          .isEqualTo(DEFAULT_GROUP_NAME);
      nodes.remove("serviceA"
          + "-" + i + ".harness.com");
    }
    assertThat(nodes).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCanaryNewNodesHelm()
      throws NoSuchAlgorithmException, KeyManagementException, IllegalAccessException {
    when(infraMappingService.get(anyString(), anyString()))
        .thenReturn(GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping()
                        .withDeploymentType(DeploymentType.HELM.name())
                        .build());
    when(serviceResourceService.getDeploymentType(any(), any(), any())).thenReturn(DeploymentType.HELM);
    List<InstanceElement> instanceElements = new ArrayList<>();
    for (int i = 0; i < 5; ++i) {
      instanceElements.add(
          anInstanceElement().hostName("serviceA-" + i + ".harness.com").workloadName("workload-" + i).build());
    }
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    CanaryWorkflowStandardParams params = Mockito.mock(CanaryWorkflowStandardParams.class);
    doReturn(instanceElements).when(params).getInstances();
    when(context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM))
        .thenReturn(PhaseElement.builder().infraMappingId(UUID.randomUUID().toString()).build());
    when(context.getAppId()).thenReturn(appId);
    when(context.fetchInfraMappingId()).thenReturn(infraMappingId);

    doReturn(params).when(context).getContextElement(ContextElementType.STANDARD);
    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));

    FieldUtils.writeField(splunkV2State, "infraMappingService", infraMappingService, true);
    FieldUtils.writeField(splunkV2State, "serviceResourceService", serviceResourceService, true);
    Map<String, String> nodes = splunkV2State.getCanaryNewHostNames(context);
    assertThat(nodes).hasSize(5);
    for (int i = 0; i < 5; ++i) {
      assertThat(nodes.keySet().contains("serviceA"
                     + "-" + i + ".harness.com"))
          .isTrue();
      assertThat(nodes.get("serviceA"
                     + "-" + i + ".harness.com"))
          .isEqualTo("workload-" + i);
      nodes.remove("serviceA"
          + "-" + i + ".harness.com");
    }
    assertThat(nodes).isEmpty();
  }
}
