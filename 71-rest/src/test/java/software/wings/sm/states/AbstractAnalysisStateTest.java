package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder.aPhaseExecutionData;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_CURRENT;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.instance.ContainerInstanceHandler;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.verification.CVActivityLogService.Logger;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGenerateDemoActivityLogs_whenStateIsSuccessful() {
    AbstractAnalysisState abstractAnalysisState = mock(AbstractAnalysisState.class, Mockito.CALLS_REAL_METHODS);
    when(abstractAnalysisState.getTimeDuration()).thenReturn("15");
    Logger activityLogger = mock(Logger.class);
    abstractAnalysisState.generateDemoActivityLogs(activityLogger, false);
    verify(activityLogger, times(46)).info(anyString(), anyLong(), anyLong());
    verify(activityLogger, times(1)).info(eq("Analysis successful"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGenerateDemoActivityLogs_whenStateFailed() {
    AbstractAnalysisState abstractAnalysisState = mock(AbstractAnalysisState.class, Mockito.CALLS_REAL_METHODS);
    when(abstractAnalysisState.getTimeDuration()).thenReturn("15");
    Logger activityLogger = mock(Logger.class);
    abstractAnalysisState.generateDemoActivityLogs(activityLogger, true);
    verify(activityLogger, times(45)).info(anyString(), anyLong(), anyLong());
    verify(activityLogger, times(1)).error(anyString(), anyLong(), anyLong());
    verify(activityLogger, times(1)).error(eq("Analysis failed"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGenerateDemoThirdPartyApiCallLogs_whenStateIsSuccessful() throws IllegalAccessException {
    AbstractAnalysisState abstractAnalysisState = mock(AbstractAnalysisState.class, Mockito.CALLS_REAL_METHODS);
    WingsPersistence wingsPersistence = mock(WingsPersistence.class);
    FieldUtils.writeField(abstractAnalysisState, "wingsPersistence", wingsPersistence, true);
    when(abstractAnalysisState.getTimeDuration()).thenReturn("15");
    String accountId = generateUuid();
    String stateExecutionId = generateUuid();
    abstractAnalysisState.generateDemoThirdPartyApiCallLogs(
        accountId, stateExecutionId, false, "request body", "response body");

    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(wingsPersistence).save(argumentCaptor.capture());
    List<ThirdPartyApiCallLog> savedCallLogs = argumentCaptor.getValue();
    assertThat(savedCallLogs).hasSize(15);
    savedCallLogs.forEach(callLog -> {
      assertThat(callLog.getStateExecutionId()).isEqualTo(stateExecutionId);
      assertThat(callLog.getAccountId()).isEqualTo(accountId);
      assertThat(callLog.getRequest()).hasSize(2);
      assertThat(callLog.getRequest().get(1).getValue()).isEqualTo("request body");
      assertThat(callLog.getRequest().get(1).getType()).isEqualTo(FieldType.JSON);
      assertThat(callLog.getResponse()).hasSize(2);
      assertThat(callLog.getRequest().get(0).getType()).isEqualTo(FieldType.URL);
      assertThat(callLog.getResponse().get(1).getType()).isEqualTo(FieldType.JSON);
      assertThat(callLog.getTitle()).isEqualTo("Demo third party API call log");
      assertThat(callLog.getResponse().get(0).getValue()).isEqualTo("200");
      assertThat(callLog.getResponse().get(1).getValue()).isEqualTo("response body");
    });
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGenerateDemoThirdPartyApiCallLogs_whenStateFailed() throws IllegalAccessException {
    AbstractAnalysisState abstractAnalysisState = mock(AbstractAnalysisState.class, Mockito.CALLS_REAL_METHODS);
    WingsPersistence wingsPersistence = mock(WingsPersistence.class);
    FieldUtils.writeField(abstractAnalysisState, "wingsPersistence", wingsPersistence, true);
    when(abstractAnalysisState.getTimeDuration()).thenReturn("15");
    String accountId = generateUuid();
    String stateExecutionId = generateUuid();
    abstractAnalysisState.generateDemoThirdPartyApiCallLogs(
        accountId, stateExecutionId, true, "request body", "response body");
    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(wingsPersistence).save(argumentCaptor.capture());
    List<ThirdPartyApiCallLog> savedCallLogs = argumentCaptor.getValue();
    assertThat(savedCallLogs).hasSize(15);
    for (int minute = 0; minute < 15; minute++) {
      ThirdPartyApiCallLog callLog = savedCallLogs.get(minute);
      assertThat(callLog.getStateExecutionId()).isEqualTo(stateExecutionId);
      assertThat(callLog.getAccountId()).isEqualTo(accountId);
      assertThat(callLog.getRequest()).hasSize(2);
      assertThat(callLog.getRequest().get(1).getValue()).isEqualTo("request body");
      assertThat(callLog.getRequest().get(1).getType()).isEqualTo(FieldType.JSON);
      assertThat(callLog.getResponse()).hasSize(2);
      assertThat(callLog.getRequest().get(0).getType()).isEqualTo(FieldType.URL);
      assertThat(callLog.getResponse().get(1).getType()).isEqualTo(FieldType.JSON);
      assertThat(callLog.getTitle()).isEqualTo("Demo third party API call log");
      if (minute == 15 / 2) {
        assertThat(callLog.getResponse().get(0).getValue()).isEqualTo("408");
        assertThat(callLog.getResponse().get(1).getValue()).isEqualTo("Timeout from service provider");
      } else {
        assertThat(callLog.getResponse().get(0).getValue()).isEqualTo("200");
        assertThat(callLog.getResponse().get(1).getValue()).isEqualTo("response body");
      }
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGetLastExecutionNodes() throws IllegalAccessException {
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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetLastExecutionNodesWithPhase() throws IllegalAccessException {
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGetCanaryNewNodes() throws IllegalAccessException {
    List<InstanceElement> instanceElements = new ArrayList<>();
    for (int i = 0; i < 5; ++i) {
      instanceElements.add(
          anInstanceElement().hostName("serviceA-" + i + ".harness.com").workloadName("workload-" + i).build());
    }
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    CanaryWorkflowStandardParams params = Mockito.mock(CanaryWorkflowStandardParams.class);
    doReturn(instanceElements).when(params).getInstances();
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM))
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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetCanaryNewNodesHelm() throws IllegalAccessException {
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
    when(context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM))
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

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSampleHosts_whenFFIsDisabled() throws IllegalAccessException {
    AbstractAnalysisState abstractAnalysisState = mock(AbstractAnalysisState.class, Mockito.CALLS_REAL_METHODS);
    String accountId = generateUuid();
    FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
    FieldUtils.writeField(abstractAnalysisState, "featureFlagService", featureFlagService, true);
    when(featureFlagService.isEnabled(eq(FeatureName.CV_HOST_SAMPLING), eq(accountId))).thenReturn(false);
    AnalysisContext analysisContext = AnalysisContext.builder()
                                          .accountId(accountId)
                                          .testNodes(getHostsMap(20, 5))
                                          .controlNodes(getHostsMap(20, 5))
                                          .build();
    abstractAnalysisState.sampleHostsMap(analysisContext);
    assertThat(analysisContext.getControlNodes()).hasSize(100);
    assertThat(analysisContext.getTestNodes()).hasSize(100);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSampleHosts_whenFFIsEnabledAndNoSamplingRequired() throws IllegalAccessException {
    AbstractAnalysisState abstractAnalysisState = mock(AbstractAnalysisState.class, Mockito.CALLS_REAL_METHODS);
    String accountId = generateUuid();
    FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
    FieldUtils.writeField(abstractAnalysisState, "featureFlagService", featureFlagService, true);
    when(featureFlagService.isEnabled(eq(FeatureName.CV_HOST_SAMPLING), eq(accountId))).thenReturn(true);
    AnalysisContext analysisContext = AnalysisContext.builder()
                                          .accountId(accountId)
                                          .testNodes(getHostsMap(10, 5))
                                          .controlNodes(getHostsMap(9, 5))
                                          .build();
    Map<String, String> testNodes = analysisContext.getTestNodes();
    Map<String, String> controlNodes = analysisContext.getControlNodes();
    abstractAnalysisState.sampleHostsMap(analysisContext);
    assertThat(analysisContext.getControlNodes()).hasSize(45);
    assertThat(analysisContext.getTestNodes()).hasSize(50);
    assertThat(analysisContext.getTestNodes()).isEqualTo(testNodes);
    assertThat(analysisContext.getControlNodes()).isEqualTo(controlNodes);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSampleHosts_whenFFIsEnabledAndSamplingRequired() throws IllegalAccessException {
    AbstractAnalysisState abstractAnalysisState = mock(AbstractAnalysisState.class, Mockito.CALLS_REAL_METHODS);
    String accountId = generateUuid();
    FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
    FieldUtils.writeField(abstractAnalysisState, "featureFlagService", featureFlagService, true);
    when(featureFlagService.isEnabled(eq(FeatureName.CV_HOST_SAMPLING), eq(accountId))).thenReturn(true);
    AnalysisContext analysisContext = AnalysisContext.builder()
                                          .accountId(accountId)
                                          .testNodes(getHostsMap(20, 5))
                                          .controlNodes(getHostsMap(11, 5))
                                          .build();
    abstractAnalysisState.sampleHostsMap(analysisContext);
    assertThat(analysisContext.getControlNodes()).hasSize(50);
    assertThat(analysisContext.getTestNodes()).hasSize(50);
  }

  private Map<String, String> getHostsMap(int numberOfHostsPerGroup, int numberOfGroups) {
    Map<String, String> hosts = new HashMap<>();
    for (int i = 0; i < numberOfGroups; i++) {
      String groupName = generateUuid();
      for (int j = 0; j < numberOfHostsPerGroup; j++) {
        hosts.put(generateUuid(), groupName);
      }
    }
    return hosts;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testIsNewInstanceFieldPopulated_False() {
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    SplunkV2State state = spy(new SplunkV2State("SplunkState"));
    InfrastructureMapping mapping = new PcfInfrastructureMapping();

    doReturn(mapping).when(state).getInfrastructureMapping(context);
    boolean isPopulated = state.isNewInstanceFieldPopulated(context);
    assertThat(isPopulated).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testIsNewInstanceFieldPopulated_AmiAsgDeployment() {
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    SplunkV2State state = spy(new SplunkV2State("SplunkState"));
    AwsAmiInfrastructureMapping mapping = new AwsAmiInfrastructureMapping();
    mapping.setAmiDeploymentType(AmiDeploymentType.AWS_ASG);

    doReturn(mapping).when(state).getInfrastructureMapping(context);
    boolean isPopulated = state.isNewInstanceFieldPopulated(context);
    assertThat(isPopulated).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testIsNewInstanceFieldPopulated_SpotinstDeployment() {
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    SplunkV2State state = spy(new SplunkV2State("SplunkState"));
    AwsAmiInfrastructureMapping mapping = new AwsAmiInfrastructureMapping();
    mapping.setAmiDeploymentType(AmiDeploymentType.SPOTINST);

    doReturn(mapping).when(state).getInfrastructureMapping(context);
    boolean isPopulated = state.isNewInstanceFieldPopulated(context);
    assertThat(isPopulated).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testPopulateNewAndOldHostNames_EmptyInstances() {
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    SplunkV2State state = spy(new SplunkV2State("SplunkState"));
    Map<String, String> controlNodes = new HashMap<>();
    Map<String, String> testNodes = new HashMap<>();

    AwsAmiInfrastructureMapping mapping = new AwsAmiInfrastructureMapping();
    mapping.setAmiDeploymentType(AmiDeploymentType.SPOTINST);
    doReturn(mapping).when(state).getInfrastructureMapping(context);

    WorkflowStandardParams params = Mockito.mock(WorkflowStandardParams.class);
    doReturn(new ArrayList<>()).when(params).getInstances();
    doReturn(params).when(context).getContextElement(ContextElementType.STANDARD);

    assertThatThrownBy(() -> state.populateNewAndOldHostNames(context, controlNodes, testNodes))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testPopulateNewAndOldHostNames_NonEmptyInstances_Canary() throws IllegalAccessException {
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    SplunkV2State state = spy(new SplunkV2State("SplunkState"));
    Map<String, String> controlNodes = new HashMap<>();
    Map<String, String> testNodes = new HashMap<>();

    AwsAmiInfrastructureMapping mapping = new AwsAmiInfrastructureMapping();
    mapping.setAmiDeploymentType(AmiDeploymentType.SPOTINST);
    doReturn(mapping).when(state).getInfrastructureMapping(context);

    String newHostName = "newHost";
    String oldHostName = "oldHost";

    WorkflowStandardParams params = Mockito.mock(WorkflowStandardParams.class);
    InstanceElement instanceElementNew = new InstanceElement();
    instanceElementNew.setNewInstance(true);
    instanceElementNew.setHostName(newHostName);

    InstanceElement instanceElementOld = new InstanceElement();
    instanceElementOld.setNewInstance(false);
    instanceElementOld.setHostName(oldHostName);

    doReturn(Arrays.asList(instanceElementNew, instanceElementOld)).when(params).getInstances();
    doReturn(params).when(context).getContextElement(ContextElementType.STANDARD);
    doReturn(COMPARE_WITH_CURRENT).when(state).getComparisonStrategy();
    doReturn(DeploymentType.AMI).when(serviceResourceService).getDeploymentType(any(), any(), any());
    FieldUtils.writeField(state, "serviceResourceService", serviceResourceService, true);

    state.populateNewAndOldHostNames(context, controlNodes, testNodes);

    assertThat(controlNodes.size()).isEqualTo(1);
    assertThat(testNodes.size()).isEqualTo(1);

    assertThat(controlNodes.keySet()).isEqualTo(new HashSet<>(Collections.singletonList(oldHostName)));
    assertThat(testNodes.keySet()).isEqualTo(new HashSet<>(Collections.singletonList(newHostName)));

    assertThat(controlNodes.get(oldHostName)).isEqualTo(DEFAULT_GROUP_NAME);
    assertThat(testNodes.get(newHostName)).isEqualTo(DEFAULT_GROUP_NAME);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testPopulateNewAndOldHostNames_NonEmptyInstances_Previous() throws IllegalAccessException {
    ExecutionContext context = Mockito.mock(ExecutionContext.class);
    SplunkV2State state = spy(new SplunkV2State("SplunkState"));
    Map<String, String> controlNodes = new HashMap<>();
    Map<String, String> testNodes = new HashMap<>();

    AwsAmiInfrastructureMapping mapping = new AwsAmiInfrastructureMapping();
    mapping.setAmiDeploymentType(AmiDeploymentType.SPOTINST);
    doReturn(mapping).when(state).getInfrastructureMapping(context);

    String newHostName = "newHost";
    String oldHostName = "oldHost";

    WorkflowStandardParams params = Mockito.mock(WorkflowStandardParams.class);
    InstanceElement instanceElementNew = new InstanceElement();
    instanceElementNew.setNewInstance(true);
    instanceElementNew.setHostName(newHostName);

    InstanceElement instanceElementOld = new InstanceElement();
    instanceElementOld.setNewInstance(false);
    instanceElementOld.setHostName(oldHostName);

    doReturn(Arrays.asList(instanceElementNew, instanceElementOld)).when(params).getInstances();
    doReturn(params).when(context).getContextElement(ContextElementType.STANDARD);
    doReturn(COMPARE_WITH_PREVIOUS).when(state).getComparisonStrategy();
    doReturn(DeploymentType.AMI).when(serviceResourceService).getDeploymentType(any(), any(), any());
    FieldUtils.writeField(state, "serviceResourceService", serviceResourceService, true);

    state.populateNewAndOldHostNames(context, controlNodes, testNodes);

    assertThat(controlNodes.size()).isEqualTo(0);
    assertThat(testNodes.size()).isEqualTo(1);

    assertThat(testNodes.keySet()).isEqualTo(new HashSet<>(Collections.singletonList(newHostName)));
    assertThat(testNodes.get(newHostName)).isEqualTo(DEFAULT_GROUP_NAME);
  }
}
