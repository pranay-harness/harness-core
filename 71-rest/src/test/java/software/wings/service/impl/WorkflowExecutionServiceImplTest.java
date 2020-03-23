package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.WAITING;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.threading.Poller.pollFor;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;
import static software.wings.beans.FeatureName.DEPLOYMENT_TAGS;
import static software.wings.beans.FeatureName.INFRA_MAPPING_REFACTOR;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.infra.InfraDefinitionTestConstants.RESOURCE_CONSTRAINT_NAME;
import static software.wings.settings.SettingValue.SettingVariableTypes.KUBERNETES;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateType.EMAIL;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import io.harness.threading.Poller;
import io.harness.waiter.OrchestrationNotifyEventListener;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.WorkflowElement;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.FeatureName;
import software.wings.beans.Graph;
import software.wings.beans.GraphNode;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.NameValuePair;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.beans.concurrency.ConcurrencyStrategy.UnitType;
import software.wings.beans.concurrency.ConcurrentExecutionResponse;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.rules.Listeners;
import software.wings.scheduler.BackgroundJobScheduler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.HoldingScope;
import software.wings.utils.ArtifactType;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The type Workflow service impl test.
 *
 * @author Rishi
 */
@Listeners({OrchestrationNotifyEventListener.class, ExecutionEventListener.class})
@Slf4j
public class WorkflowExecutionServiceImplTest extends WingsBaseTest {
  private static final SecureRandom random = new SecureRandom();

  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Mock private BackgroundJobScheduler jobScheduler;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ResourceConstraintService resourceConstraintService;
  @Inject @InjectMocks private AccountService accountService;
  @Inject @InjectMocks private LicenseService licenseService;
  @Inject @InjectMocks private WorkflowExecutionService workflowExecutionService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @Mock private ArtifactService artifactService;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Mock private MultiArtifactWorkflowExecutionServiceHelper multiArtifactWorkflowExecutionServiceHelper;

  @Inject private ServiceInstanceService serviceInstanceService;

  @Rule public ExpectedException thrown = ExpectedException.none();

  private static final String PHASE_PARAM = "PHASE_PARAM";

  private Account account;
  private Application app;
  private Environment env;

  @Before
  public void setup() {
    when(jobScheduler.deleteJob(any(), any())).thenReturn(false);
    when(jobScheduler.scheduleJob(any(), any())).thenReturn(null);
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setLicenseUnits(10);
    account = accountService.save(Account.Builder.anAccount()
                                      .withCompanyName(COMPANY_NAME)
                                      .withAccountName(ACCOUNT_NAME)
                                      .withLicenseInfo(licenseInfo)
                                      .build(),
        false);
    app = wingsPersistence.saveAndGet(
        Application.class, anApplication().name(APP_NAME).accountId(account.getUuid()).build());
    env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    Artifact artifact = anArtifact()
                            .withAccountId(ACCOUNT_ID)
                            .withAppId(app.getAppId())
                            .withUuid(generateUuid())
                            .withDisplayName(ARTIFACT_NAME)
                            .build();
    when(artifactService.listByAppId(anyString())).thenReturn(singletonList(artifact));
    when(artifactService.listByIds(any(), any())).thenReturn(singletonList(artifact));
  }

  /**
   * Should trigger complex workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldTriggerComplexWorkflow() throws InterruptedException {
    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host host2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service1 = addService("svc1");
    Service service2 = addService("svc2");
    ServiceTemplate serviceTemplate1 = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service1.getUuid())
            .withName(service1.getName())
            .withAppId(app.getUuid())
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
    ServiceTemplate serviceTemplate2 = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service2.getUuid())
            .withName(service2.getName())
            .withAppId(app.getUuid())
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    ServiceInstance.Builder builder1 =
        aServiceInstance().withServiceTemplate(serviceTemplate1).withAppId(app.getUuid()).withEnvId(env.getUuid());
    ServiceInstance.Builder builder2 =
        aServiceInstance().withServiceTemplate(serviceTemplate2).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst11 = serviceInstanceService.save(builder1.withHost(host1).build());
    ServiceInstance inst12 = serviceInstanceService.save(builder1.withHost(host2).build());
    ServiceInstance inst21 = serviceInstanceService.save(builder2.withHost(host1).build());
    ServiceInstance inst22 = serviceInstanceService.save(builder2.withHost(host2).build());

    Graph graph =
        aGraph()
            .addNodes(getGraphNode("Repeat By Services", ExecutionStrategy.SERIAL),
                GraphNode.builder()
                    .id("RepeatByInstances")
                    .name("RepeatByInstances")
                    .type(StateType.REPEAT.name())
                    .properties(ImmutableMap.<String, Object>builder()
                                    .put("repeatElementExpression", "${instances}")
                                    .put("executionStrategy", ExecutionStrategy.PARALLEL)
                                    .build())
                    .build(),
                GraphNode.builder()
                    .id("svcRepeatWait")
                    .name("svcRepeatWait")
                    .type(StateType.WAIT.name())
                    .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                    .build(),
                GraphNode.builder()
                    .id("instRepeatWait")
                    .name("instRepeatWait")
                    .type(StateType.WAIT.name())
                    .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                    .build(),
                GraphNode.builder()
                    .id("instSuccessWait")
                    .name("instSuccessWait")
                    .type(StateType.WAIT.name())
                    .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                    .build())
            .addLinks(
                aLink().withId("l1").withFrom("Repeat By Services").withTo("svcRepeatWait").withType("repeat").build())
            .addLinks(
                aLink().withId("l2").withFrom("svcRepeatWait").withTo("RepeatByInstances").withType("success").build())
            .addLinks(
                aLink().withId("l3").withFrom("RepeatByInstances").withTo("instRepeatWait").withType("repeat").build())
            .addLinks(aLink()
                          .withId("l4")
                          .withFrom("RepeatByInstances")
                          .withTo("instSuccessWait")
                          .withType("success")
                          .build())
            .build();

    Workflow workflow =
        aWorkflow()
            .envId(env.getUuid())
            .appId(app.getUuid())
            .name("workflow1")
            .description("Sample Workflow")
            .orchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .workflowType(WorkflowType.ORCHESTRATION)
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    callback.await(ofSeconds(45));

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    assertThat(execution.getKeywords())
        .contains(workflow.getName().toLowerCase(), app.getName().toLowerCase(),
            env.getEnvironmentType().name().toLowerCase(), WorkflowType.ORCHESTRATION.name().toLowerCase());

    assertThat(execution.getExecutionNode())
        .isNotNull()
        .extracting("name", "type", "status")
        .containsExactly("Repeat By Services", "REPEAT", "SUCCESS");
    assertThat(execution.getExecutionNode().getGroup()).isNotNull();
    assertThat(execution.getExecutionNode().getGroup().getElements()).isNotNull().doesNotContainNull().hasSize(2);

    List<GraphNode> svcElements = execution.getExecutionNode().getGroup().getElements();
    assertThat(svcElements).isNotNull().hasSize(2).extracting("name").contains(service1.getName(), service2.getName());
    assertThat(svcElements).extracting("type").contains("ELEMENT", "ELEMENT");

    List<GraphNode> svcRepeatWaits = svcElements.stream().map(GraphNode::getNext).collect(toList());
    assertThat(svcRepeatWaits).isNotNull().hasSize(2).extracting("name").contains("svcRepeatWait", "svcRepeatWait");
    assertThat(svcRepeatWaits).extracting("type").contains("WAIT", "WAIT");

    List<GraphNode> repeatInstance = svcRepeatWaits.stream().map(GraphNode::getNext).collect(toList());
    assertThat(repeatInstance)
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("RepeatByInstances", "RepeatByInstances");
    assertThat(repeatInstance).extracting("type").contains("REPEAT", "REPEAT");

    List<GraphNode> instSuccessWait = repeatInstance.stream().map(GraphNode::getNext).collect(toList());
    assertThat(instSuccessWait)
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("instSuccessWait", "instSuccessWait");
    assertThat(instSuccessWait).extracting("type").contains("WAIT", "WAIT");

    // TODO: decode this logic and fix it
    //    List<GraphNode> instRepeatElements = repeatInstance.stream()
    //                                             .map(GraphNode::getGroup)
    //                                             .flatMap(group -> group.getElements().stream())
    //                                             .collect(toList());
    //    assertThat(instRepeatElements).extracting("type").contains("ELEMENT", "ELEMENT", "ELEMENT", "ELEMENT");

    //    List<GraphNode> instRepeatWait = instRepeatElements.stream().map(GraphNode::getNext).collect(toList());
    //    assertThat(instRepeatWait)
    //        .isNotNull()
    //        .hasSize(4)
    //        .extracting("name")
    //        .contains("instRepeatWait", "instRepeatWait", "instRepeatWait", "instRepeatWait");
    //    assertThat(instRepeatWait).extracting("type").contains("WAIT", "WAIT", "WAIT", "WAIT");
  }

  /**
   * Trigger pipeline.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void triggerPipeline() throws InterruptedException {
    Service service = addService("svc1");

    Pipeline pipeline = constructPipeline(service);

    triggerPipeline(app.getUuid(), pipeline);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTriggerPipelineWithDeploymentMetaDataFFOn() {
    Service service = addService("svc1");

    Pipeline pipeline = constructPipeline(service);
    Artifact artifact =
        Artifact.Builder.anArtifact().withAccountId(ACCOUNT_ID).withAppId(APP_ID).withUuid(ARTIFACT_ID).build();
    List<ArtifactVariable> variables = singletonList(ArtifactVariable.builder()
                                                         .name(VARIABLE_NAME)
                                                         .entityId(ENTITY_ID)
                                                         .type(VariableType.ARTIFACT)
                                                         .value(artifact.getUuid())
                                                         .build());
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifactVariables(variables);
    executionArgs.setWorkflowType(WorkflowType.PIPELINE);
    executionArgs.setPipelineId(pipeline.getUuid());
    WorkflowExecution workflowExecution =
        workflowExecutionService.triggerEnvExecution(pipeline.getAppId(), null, executionArgs, null);

    assertThat(workflowExecution.getExecutionArgs().getArtifactVariables()).isNotNull();
  }

  private Pipeline constructPipeline(Service service) {
    Host host = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host").build());

    ServiceTemplate serviceTemplate = getServiceTemplate(service);

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst = serviceInstanceService.save(builder.withHost(host).build());

    Graph graph =
        aGraph()
            .addNodes(getGraphNode("Repeat By Services", ExecutionStrategy.SERIAL),
                GraphNode.builder()
                    .id("RepeatByInstances")
                    .name("RepeatByInstances")
                    .type(StateType.REPEAT.name())
                    .properties(ImmutableMap.<String, Object>builder()
                                    .put("repeatElementExpression", "${instances}")
                                    .put("executionStrategy", ExecutionStrategy.PARALLEL)
                                    .build())
                    .build(),
                GraphNode.builder()
                    .id("svcRepeatWait")
                    .name("svcRepeatWait")
                    .type(StateType.WAIT.name())
                    .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                    .build(),
                GraphNode.builder()
                    .id("instRepeatWait")
                    .name("instRepeatWait")
                    .type(StateType.WAIT.name())
                    .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                    .build(),
                GraphNode.builder()
                    .id("instSuccessWait")
                    .name("instSuccessWait")
                    .type(StateType.WAIT.name())
                    .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                    .build())
            .addLinks(
                aLink().withId("l1").withFrom("Repeat By Services").withTo("svcRepeatWait").withType("repeat").build())
            .addLinks(
                aLink().withId("l2").withFrom("svcRepeatWait").withTo("RepeatByInstances").withType("success").build())
            .addLinks(
                aLink().withId("l3").withFrom("RepeatByInstances").withTo("instRepeatWait").withType("repeat").build())
            .addLinks(aLink()
                          .withId("l4")
                          .withFrom("RepeatByInstances")
                          .withTo("instSuccessWait")
                          .withType("success")
                          .build())
            .build();

    Workflow workflow =
        aWorkflow()
            .envId(env.getUuid())
            .appId(app.getUuid())
            .name("workflow1")
            .description("Sample Workflow")
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();

    PipelineStage stag1 =
        PipelineStage.builder()
            .pipelineStageElements(
                asList(PipelineStageElement.builder()
                           .name("DEV")
                           .type(ENV_STATE.name())
                           .properties(ImmutableMap.of("envId", env.getUuid(), "workflowId", workflow.getUuid()))
                           .build()))
            .build();

    List<PipelineStage> pipelineStages = asList(stag1);

    Pipeline pipeline = Pipeline.builder()
                            .appId(app.getUuid())
                            .name("pipeline1")
                            .description("Sample Pipeline")
                            .pipelineStages(pipelineStages)
                            .build();

    pipeline = pipelineService.save(pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();
    return pipeline;
  }

  private WorkflowExecution triggerPipeline(String appId, Pipeline pipeline) throws InterruptedException {
    Artifact artifact = wingsPersistence.saveAndGet(
        Artifact.class, anArtifact().withAppId(app.getUuid()).withDisplayName(ARTIFACT_NAME).build());
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(asList(artifact));

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution =
        ((WorkflowExecutionServiceImpl) workflowExecutionService)
            .triggerPipelineExecution(appId, pipeline.getUuid(), executionArgs, callback, null);
    callback.await(ofSeconds(15));

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Pipeline executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId, true);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrProperty("displayName")
        .hasFieldOrProperty("releaseNo")
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);

    return execution;
  }

  /**
   * Should trigger workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void shouldTriggerWorkflow() throws InterruptedException {
    String appId = app.getUuid();
    triggerWorkflow(appId, env, "workflow1");
  }

  /**
   * Should trigger workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldTriggerWorkflowWithRelease() throws InterruptedException {
    String appId = app.getUuid();
    Workflow workflow = createExecutableWorkflow(appId, env, "workflow1");
    WorkflowExecution workflowExecution = triggerWorkflow(workflow, env);
    assertThat(workflowExecution).isNotNull().hasFieldOrPropertyWithValue("releaseNo", "1");

    WorkflowElement workflowElement = getWorkflowElement(appId, workflowExecution);
    assertThat(workflowElement).isNotNull().hasFieldOrPropertyWithValue("releaseNo", "1");

    WorkflowExecution workflowExecution2 = triggerWorkflow(workflow, env);
    assertThat(workflowExecution2).isNotNull().hasFieldOrPropertyWithValue("releaseNo", "2");
    workflowElement = getWorkflowElement(appId, workflowExecution2);
    assertThat(workflowElement)
        .isNotNull()
        .extracting(
            "releaseNo", "displayName", "lastGoodDeploymentUuid", "lastGoodDeploymentDisplayName", "lastGoodReleaseNo")
        .containsExactly(workflowExecution2.getReleaseNo(), workflowExecution2.displayName(),
            workflowExecution.getUuid(), workflowExecution.displayName(), workflowExecution.getReleaseNo());
  }

  private WorkflowElement getWorkflowElement(String appId, WorkflowExecution workflowExecution) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.appId, appId)
            .filter(StateExecutionInstanceKeys.executionUuid, workflowExecution.getUuid())
            .get();

    assertThat(stateExecutionInstance).isNotNull();
    assertThat(stateExecutionInstance.getContextElements()).isNotNull();
    Optional<ContextElement> first =
        stateExecutionInstance.getContextElements()
            .stream()
            .filter(contextElement -> contextElement.getElementType() == ContextElementType.STANDARD)
            .findFirst();
    assertThat(first.isPresent()).isTrue();
    assertThat(first.get()).isInstanceOf(WorkflowStandardParams.class).hasFieldOrProperty("workflowElement");

    return ((WorkflowStandardParams) first.get()).getWorkflowElement();
  }
  /**
   * Should get node details.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetNodeDetails() throws InterruptedException {
    String appId = app.getUuid();

    final WorkflowExecution triggerWorkflow = triggerWorkflow(appId, env, "workflow1");
    WorkflowExecution execution = workflowExecutionService.getExecutionDetails(appId, triggerWorkflow.getUuid(), true);
    GraphNode node0 = execution.getExecutionNode();
    final GraphNode executionDetailsForNode =
        workflowExecutionService.getExecutionDetailsForNode(appId, execution.getUuid(), node0.getId());
    assertThat(executionDetailsForNode).isEqualToIgnoringGivenFields(node0, "next");
  }

  /**
   * Should update failed count.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldUpdateFailedCount() throws InterruptedException {
    String appId = app.getUuid();
    triggerWorkflow(appId, env, "workflow1");
    WorkflowExecution workflowExecution =
        wingsPersistence.createQuery(WorkflowExecution.class).filter(WorkflowExecutionKeys.appId, appId).get();
    workflowExecutionService.incrementFailed(workflowExecution.getAppId(), workflowExecution.getUuid(), 1);
    workflowExecution =
        wingsPersistence.createQuery(WorkflowExecution.class).filter(WorkflowExecutionKeys.appId, appId).get();
    assertThat(workflowExecution.getBreakdown().getFailed()).isEqualTo(1);
    logger.info("shouldUpdateFailedCount test done");
  }

  /**
   * Trigger workflow.
   *
   * @param appId the app id
   * @param env   the env
   * @return the string
   * @throws InterruptedException the interrupted exception
   */
  public WorkflowExecution triggerWorkflow(String appId, Environment env, String workflowName)
      throws InterruptedException {
    Workflow workflow = createExecutableWorkflow(appId, env, workflowName);
    return triggerWorkflow(workflow, env);
  }

  /**
   * Trigger workflow.
   *
   * @param env   the env
   * @return the string
   * @throws InterruptedException the interrupted exception
   */
  public WorkflowExecution triggerWorkflow(Workflow workflow, Environment env) throws InterruptedException {
    String appId = workflow.getAppId();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(
        Artifact.Builder.anArtifact().withAccountId(ACCOUNT_ID).withAppId(APP_ID).withUuid(ARTIFACT_ID).build()));

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    callback.await(ofSeconds(15));

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId, true);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrProperty("releaseNo")
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    return execution;
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldTriggerWorkflowFailForExpiredTrialLicense() throws InterruptedException {
    shouldTriggerWorkflowFailForExpiredLicense(AccountType.TRIAL);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldTriggerWorkflowFailForExpiredPaidLicense() throws InterruptedException {
    shouldTriggerWorkflowFailForExpiredLicense(AccountType.PAID);
  }

  private void shouldTriggerWorkflowFailForExpiredLicense(String accountType) throws InterruptedException {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(accountType);
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    licenseInfo.setExpiryTime(System.currentTimeMillis() + 5000);
    account.setLicenseInfo(licenseInfo);
    licenseService.updateAccountLicense(account.getUuid(), licenseInfo);

    Thread.sleep(10000);
    Workflow workflow = createExecutableWorkflow(app.getUuid(), env, "workflow1");
    String appId = workflow.getAppId();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(asList(Artifact.Builder.anArtifact().withAppId(APP_ID).withUuid(ARTIFACT_ID).build()));

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    thrown.expect(WingsException.class);
    workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);

    // Scenario 2, update the license to be valid and test again.

    licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(accountType);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(System.currentTimeMillis() + 1000000);
    account.setLicenseInfo(licenseInfo);
    licenseService.updateAccountLicense(account.getUuid(), licenseInfo);

    WorkflowExecution workflowExecution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    assertThat(workflowExecution).isNotNull();
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldTriggerPipelineFailForExpiredTrialLicense() throws InterruptedException {
    shouldTriggerPipelineFailForExpiredLicense(AccountType.TRIAL);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void shouldTriggerPipelineFailForExpiredPaidLicense() throws InterruptedException {
    shouldTriggerPipelineFailForExpiredLicense(AccountType.PAID);
  }

  private void shouldTriggerPipelineFailForExpiredLicense(String accountType) throws InterruptedException {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(accountType);
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    licenseInfo.setExpiryTime(System.currentTimeMillis() + 5000);
    account.setLicenseInfo(licenseInfo);
    licenseService.updateAccountLicense(account.getUuid(), licenseInfo);

    Thread.sleep(10000);
    Service service = addService("svc1");

    Pipeline pipeline = constructPipeline(service);
    thrown.expect(WingsException.class);
    triggerPipeline(app.getUuid(), pipeline);

    // Scenario 2, update the license to be valid and test again.
    licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(accountType);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(System.currentTimeMillis() + 1000000);
    account.setLicenseInfo(licenseInfo);
    licenseService.updateAccountLicense(account.getUuid(), licenseInfo);

    WorkflowExecution workflowExecution = triggerPipeline(app.getUuid(), pipeline);
    assertThat(workflowExecution).isNotNull();
  }

  private Workflow createExecutableWorkflow(String appId, Environment env, String workflowName) {
    Graph graph = getGraphForExecutableWorkflow();

    Workflow workflow =
        aWorkflow()
            .envId(env.getUuid())
            .appId(appId)
            .name(workflowName)
            .description("Sample Workflow")
            .orchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .workflowType(WorkflowType.ORCHESTRATION)
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();
    return workflow;
  }

  private Workflow createExecutableWorkflowWithThrottling(String appId, Environment env, String workflowName) {
    Graph graph = getGraphForExecutableWorkflow();

    Workflow workflow = aWorkflow()
                            .envId(env.getUuid())
                            .appId(appId)
                            .name(workflowName)
                            .description("Sample Workflow")
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                                       .withGraph(graph)
                                                       .withConcurrencyStrategy(ConcurrencyStrategy.builder().build())
                                                       .build())
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();
    return workflow;
  }

  private Graph getGraphForExecutableWorkflow() {
    return aGraph()
        .addNodes(GraphNode.builder()
                      .id("n1")
                      .name("wait")
                      .type(StateType.WAIT.name())
                      .properties(ImmutableMap.<String, Object>builder().put("duration", 1L).build())
                      .origin(true)
                      .build(),
            GraphNode.builder()
                .id("n2")
                .name("email")
                .type(EMAIL.name())
                .properties(ImmutableMap.<String, Object>builder()
                                .put("toAddress", "a@b.com")
                                .put("subject", "testing")
                                .build())
                .build())
        .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
        .build();
  }

  /**
   * Should list workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void shouldListWorkflow() throws InterruptedException {
    String appId = app.getUuid();

    triggerWorkflow(appId, env, "workflow1");

    // 2nd workflow
    createExecutableWorkflow(appId, env, "workflow2");
    PageRequest<Workflow> pageRequest = aPageRequest().addFilter(Workflow.APP_ID_KEY, EQ, appId).build();
    PageResponse<Workflow> res = workflowService.listWorkflows(pageRequest, null, false, null);

    assertThat(res).isNotNull().hasSize(2);
  }

  /**
   * Should pause and resume
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldPauseAndResumeState() throws InterruptedException {
    Graph graph = getAbortedGraph();

    Workflow workflow =
        aWorkflow()
            .envId(env.getUuid())
            .appId(app.getUuid())
            .name("workflow1")
            .description("Sample Workflow")
            .orchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .workflowType(WorkflowType.ORCHESTRATION)
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    pollFor(ofSeconds(10), ofMillis(100), () -> {
      final WorkflowExecution pull = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
      return pull.getStatus() == ExecutionStatus.PAUSED;
    });

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);

    assertThat(execution).isNotNull().extracting("uuid", "status").containsExactly(executionId, ExecutionStatus.PAUSED);

    assertThat(execution.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "wait1")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(execution.getExecutionNode().getNext())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "pause1")
        .hasFieldOrPropertyWithValue("status", "PAUSED");

    ExecutionInterrupt executionInterrupt =
        anExecutionInterrupt()
            .appId(app.getUuid())
            .envId(env.getUuid())
            .executionUuid(executionId)
            .stateExecutionInstanceId(execution.getExecutionNode().getNext().getId())
            .executionInterruptType(ExecutionInterruptType.RESUME)
            .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    assertThat(execution.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "wait1")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(execution.getExecutionNode().getNext())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "pause1")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(execution.getExecutionNode().getNext().getNext())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "wait2")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
  }

  /**
   * Should pause and resume
   *
   * @throws InterruptedException the interrupted exception
   */
  // TODO - Fix this, it's failing in Jenkins - almost all the time
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldPauseAllAndResumeAllState() throws InterruptedException {
    Service service1 = addService("svc1");
    Service service2 = addService("svc2");

    Graph graph = getGraph();

    Workflow workflow =
        aWorkflow()
            .envId(env.getUuid())
            .appId(app.getUuid())
            .name("workflow1")
            .description("Sample Workflow")
            .orchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .workflowType(WorkflowType.ORCHESTRATION)
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();
    ExecutionArgs executionArgs = new ExecutionArgs();
    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    pollFor(ofSeconds(3), ofMillis(100), () -> {
      final WorkflowExecution pull = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
      return pull.getStatus() == ExecutionStatus.RUNNING;
    });

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                                .executionUuid(executionId)
                                                .envId(env.getUuid())
                                                .build();
    executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    assertThat(executionInterrupt).isNotNull().hasFieldOrProperty("uuid");

    pollFor(ofSeconds(15), ofMillis(100), () -> {
      final WorkflowExecution pull = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
      return pull.getStatus() == ExecutionStatus.PAUSED && pull.getExecutionNode().getGroup() != null;
    });

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);

    List<GraphNode> wait1List = execution.getExecutionNode()
                                    .getGroup()
                                    .getElements()
                                    .stream()
                                    .filter(n -> n.getNext() != null)
                                    .map(GraphNode::getNext)
                                    .collect(toList());
    List<GraphNode> wait2List =
        wait1List.stream().filter(n -> n.getNext() != null).map(GraphNode::getNext).collect(toList());

    assertThat(execution).isNotNull().extracting("uuid", "status").containsExactly(executionId, ExecutionStatus.PAUSED);
    assertThat(execution.getExecutionNode())
        .extracting("name", "type", "status")
        .containsExactly("RepeatByServices", "REPEAT", "RUNNING");
    assertThat(wait1List)
        .filteredOn("name", "wait1")
        .hasSize(2)
        .allMatch(n -> "WAIT".equals(n.getType()) && "SUCCESS".equals(n.getStatus()));
    assertThat(wait2List)
        .filteredOn("name", "wait2")
        .hasSize(2)
        .allMatch(n -> "WAIT".equals(n.getType()) && "PAUSED".equals(n.getStatus()));

    executionInterrupt = anExecutionInterrupt()
                             .appId(app.getUuid())
                             .executionInterruptType(ExecutionInterruptType.RESUME_ALL)
                             .executionUuid(executionId)
                             .envId(env.getUuid())
                             .build();
    executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    assertThat(executionInterrupt).isNotNull().hasFieldOrProperty("uuid");

    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.SUCCESS);

    wait1List = execution.getExecutionNode()
                    .getGroup()
                    .getElements()
                    .stream()
                    .filter(n -> n.getNext() != null)
                    .map(GraphNode::getNext)
                    .collect(toList());
    wait2List = wait1List.stream().filter(n -> n.getNext() != null).map(GraphNode::getNext).collect(toList());

    assertThat(execution.getExecutionNode())
        .extracting("name", "type", "status")
        .containsExactly("RepeatByServices", "REPEAT", "SUCCESS");
    assertThat(wait1List)
        .filteredOn("name", "wait1")
        .hasSize(2)
        .allMatch(n -> "WAIT".equals(n.getType()) && "SUCCESS".equals(n.getStatus()));
    assertThat(wait2List)
        .filteredOn("name", "wait2")
        .hasSize(2)
        .allMatch(n -> "WAIT".equals(n.getType()) && "SUCCESS".equals(n.getStatus()));
  }

  private Graph getGraph() {
    return aGraph()
        .addNodes(getGraphNode("RepeatByServices", ExecutionStrategy.PARALLEL),
            GraphNode.builder()
                .id("wait1")
                .name("wait1")
                .type(StateType.WAIT.name())
                .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                .build(),
            GraphNode.builder()
                .id("wait2")
                .name("wait2")
                .type(StateType.WAIT.name())
                .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                .build())
        .addLinks(aLink().withId("l1").withFrom("RepeatByServices").withTo("wait1").withType("repeat").build())
        .addLinks(aLink().withId("l2").withFrom("wait1").withTo("wait2").withType("success").build())
        .build();
  }

  /**
   * Should throw invalid argument for invalid workflow id.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowInvalidArgumentForInvalidWorkflowId() {
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.PAUSE)
                                                .envId(env.getUuid())
                                                .executionUuid(generateUuid())
                                                .build();
    try {
      executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception.getParams().values()).doesNotContainNull();
      assertThat(exception.getParams().values().iterator().next())
          .isInstanceOf(String.class)
          .asString()
          .startsWith("No WorkflowExecution for executionUuid");
      assertThat(exception).hasMessage(ErrorCode.INVALID_ARGUMENT.name());
    }
  }

  /**
   * Should abort
   *
   * @throws InterruptedException the interrupted exception
   */
  // TODO: fix this. It seems there is production issues
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldAbortState() throws InterruptedException {
    Graph graph = getAbortedGraph();

    Workflow workflow =
        aWorkflow()
            .envId(env.getUuid())
            .appId(app.getUuid())
            .name("workflow1")
            .description("Sample Workflow")
            .orchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .workflowType(WorkflowType.ORCHESTRATION)
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    pollFor(ofSeconds(10), ofMillis(100), () -> {
      final WorkflowExecution pull = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
      return pull.getStatus() == ExecutionStatus.PAUSED;
    });

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
    assertThat(execution).isNotNull().extracting("uuid", "status").containsExactly(executionId, ExecutionStatus.PAUSED);

    assertThat(execution.getExecutionNode()).isNotNull();

    assertThat(execution.getExecutionNode().getNext()).isNotNull();

    ExecutionInterrupt executionInterrupt =
        anExecutionInterrupt()
            .appId(app.getUuid())
            .envId(env.getUuid())
            .executionUuid(executionId)
            .stateExecutionInstanceId(execution.getExecutionNode().getNext().getId())
            .executionInterruptType(ExecutionInterruptType.ABORT)
            .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.ABORTED);

    assertThat(execution.getExecutionNode()).isNotNull();
  }

  private Graph getAbortedGraph() {
    return aGraph()
        .addNodes(GraphNode.builder()
                      .id("wait1")
                      .origin(true)
                      .name("wait1")
                      .type(StateType.WAIT.name())
                      .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                      .build(),
            GraphNode.builder()
                .id("pause1")
                .name("pause1")
                .type(StateType.PAUSE.name())
                .properties(ImmutableMap.<String, Object>builder().put("toAddress", "to1").build())
                .build(),
            GraphNode.builder()
                .id("wait2")
                .name("wait2")
                .type(StateType.WAIT.name())
                .properties(ImmutableMap.<String, Object>builder().put("duration", 1).build())
                .build())
        .addLinks(aLink().withId("l1").withFrom("wait1").withTo("pause1").withType("success").build())
        .addLinks(aLink().withId("l2").withFrom("pause1").withTo("wait2").withType("success").build())
        .build();
  }

  /**
   * Should abort all
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = SRINIVAS, intermittent = true)
  @Category(UnitTests.class)
  public void shouldAbortAllStates() {
    Service service1 = addService("svc1");
    Service service2 = addService("svc2");

    Graph graph = getGraph();

    Workflow workflow =
        aWorkflow()
            .envId(env.getUuid())
            .appId(app.getUuid())
            .name("workflow1")
            .description("Sample Workflow")
            .orchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .workflowType(WorkflowType.ORCHESTRATION)
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();
    ExecutionArgs executionArgs = new ExecutionArgs();
    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    pollFor(ofSeconds(5), ofMillis(100), () -> {
      final WorkflowExecution pull = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
      return pull.getStatus() == ExecutionStatus.RUNNING;
    });

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.ABORT_ALL)
                                                .executionUuid(executionId)
                                                .envId(env.getUuid())
                                                .build();
    executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    assertThat(executionInterrupt).isNotNull().hasFieldOrProperty("uuid");

    pollFor(ofSeconds(15), ofMillis(100), () -> {
      final WorkflowExecution pull = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
      return pull.getStatus() == ExecutionStatus.ABORTED;
    });

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);

    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.ABORTED);

    assertThat(execution.getExecutionNode()).isNotNull();
  }

  /**
   * Should wait on error
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldWaitOnError() throws InterruptedException {
    Host applicationHost1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host applicationHost2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service = addService("svc1");
    ServiceTemplate serviceTemplate = getServiceTemplate(service);

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(applicationHost1).build());
    ServiceInstance inst2 = serviceInstanceService.save(builder.withHost(applicationHost2).build());

    Graph graph =
        aGraph()
            .addNodes(getGraphNode("RepeatByServices", ExecutionStrategy.PARALLEL),
                GraphNode.builder()
                    .id("RepeatByInstances")
                    .name("RepeatByInstances")
                    .type(StateType.REPEAT.name())
                    .properties(ImmutableMap.<String, Object>builder()
                                    .put("repeatElementExpression", "${instances()}")
                                    .put("executionStrategy", ExecutionStrategy.SERIAL)
                                    .build())
                    .build(),
                GraphNode.builder()
                    .id("install")
                    .name("install")
                    .type(StateType.COMMAND.name())
                    .properties(ImmutableMap.<String, Object>builder().put("command", "install").build())
                    .build())
            .addLinks(aLink()
                          .withId("l1")
                          .withFrom("RepeatByServices")
                          .withTo("RepeatByInstances")
                          .withType("repeat")
                          .build())
            .addLinks(aLink().withId("l2").withFrom("RepeatByInstances").withTo("install").withType("repeat").build())
            .build();

    Workflow workflow =
        aWorkflow()
            .envId(env.getUuid())
            .appId(app.getUuid())
            .name("workflow1")
            .description("Sample Workflow")
            .orchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .workflowType(WorkflowType.ORCHESTRATION)
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setErrorStrategy(ErrorStrategy.PAUSE);
    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationWorkflowExecution(app.getUuid(), env.getUuid(),
                                          workflow.getUuid(), null, executionArgs, callback, null);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    List<GraphNode> installNodes = getNodes(executionId);

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", WAITING);
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(WAITING.name());

    GraphNode installNode = installNodes.get(0);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .envId(env.getUuid())
                                                .executionUuid(executionId)
                                                .stateExecutionInstanceId(installNode.getId())
                                                .executionInterruptType(ExecutionInterruptType.MARK_SUCCESS)
                                                .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);

    installNodes = getNodes(executionId);

    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", WAITING);
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(2)
        .extracting("status")
        .contains(ExecutionStatus.SUCCESS.name(), WAITING.name());

    installNode = installNodes.stream()
                      .filter(n -> n.getStatus() != null && n.getStatus().equals(WAITING.name()))
                      .collect(toList())
                      .get(0);
    executionInterrupt = anExecutionInterrupt()
                             .appId(app.getUuid())
                             .envId(env.getUuid())
                             .executionUuid(executionId)
                             .stateExecutionInstanceId(installNode.getId())
                             .executionInterruptType(ExecutionInterruptType.IGNORE)
                             .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.SUCCESS);
    installNodes = execution.getExecutionNode()
                       .getGroup()
                       .getElements()
                       .stream()
                       .filter(node -> node.getNext() != null)
                       .map(GraphNode::getNext)
                       .filter(node -> node.getGroup() != null)
                       .map(GraphNode::getGroup)
                       .filter(group -> group.getElements() != null)
                       .flatMap(group -> group.getElements().stream())
                       .filter(node -> node.getNext() != null)
                       .map(GraphNode::getNext)
                       .collect(toList());
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(2)
        .extracting("status")
        .containsExactly(ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name());
  }

  private List<GraphNode> installNodes(WorkflowExecution execution) {
    return execution.getExecutionNode()
        .getGroup()
        .getElements()
        .stream()
        .map(GraphNode::getNext)
        .filter(Objects::nonNull)
        .map(GraphNode::getGroup)
        .filter(Objects::nonNull)
        .filter(group -> group.getElements() != null)
        .flatMap(group -> group.getElements().stream())
        .map(GraphNode::getNext)
        .filter(Objects::nonNull)
        .collect(toList());
  }

  private List<GraphNode> getNodes(String executionId) {
    Poller.pollFor(ofSeconds(10), ofMillis(100), () -> {
      WorkflowExecution execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
      if (execution.getExecutionNode() == null) {
        return false;
      }
      if (execution.getExecutionNode().getGroup() == null) {
        return false;
      }
      if (execution.getExecutionNode().getGroup().getElements() == null) {
        return false;
      }

      return installNodes(execution).stream().anyMatch(
          n -> n.getStatus() != null && n.getStatus().equals(WAITING.name()));
    });

    return installNodes(workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true));
  }

  /**
   * Should retry on error
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldRetryOnError() throws InterruptedException {
    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host1").build());

    Service service = addService("svc1");
    ServiceTemplate serviceTemplate = getServiceTemplate(service);

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(host1).build());

    Graph graph = constructGraph();

    Workflow workflow =
        aWorkflow()
            .envId(env.getUuid())
            .appId(app.getUuid())
            .name("workflow1")
            .description("Sample Workflow")
            .orchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .workflowType(WorkflowType.ORCHESTRATION)
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setErrorStrategy(ErrorStrategy.PAUSE);
    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    List<GraphNode> installNodes = getNodes(executionId);

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", WAITING);

    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(WAITING.name());

    GraphNode installNode = installNodes.get(0);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .envId(env.getUuid())
                                                .executionUuid(executionId)
                                                .stateExecutionInstanceId(installNode.getId())
                                                .executionInterruptType(ExecutionInterruptType.RETRY)
                                                .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);

    installNodes = getNodes(executionId);

    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", WAITING);
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(WAITING.name());

    installNode = installNodes.get(0);
    executionInterrupt = anExecutionInterrupt()
                             .appId(app.getUuid())
                             .envId(env.getUuid())
                             .executionUuid(executionId)
                             .stateExecutionInstanceId(installNode.getId())
                             .executionInterruptType(ExecutionInterruptType.MARK_SUCCESS)
                             .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.SUCCESS);

    installNodes = execution.getExecutionNode()
                       .getGroup()
                       .getElements()
                       .stream()
                       .filter(node -> node.getNext() != null)
                       .map(GraphNode::getNext)
                       .filter(node -> node.getGroup() != null)
                       .map(GraphNode::getGroup)
                       .filter(group -> group.getElements() != null)
                       .flatMap(group -> group.getElements().stream())
                       .filter(node -> node.getNext() != null)
                       .map(GraphNode::getNext)
                       .collect(toList());
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(ExecutionStatus.SUCCESS.name());
  }

  private Graph constructGraph() {
    return aGraph()
        .addNodes(getGraphNode("RepeatByServices", ExecutionStrategy.PARALLEL),
            GraphNode.builder()
                .id("RepeatByInstances")
                .name("RepeatByInstances")
                .type(StateType.REPEAT.name())
                .properties(ImmutableMap.<String, Object>builder()
                                .put("repeatElementExpression", "${instances()}")
                                .put("executionStrategy", ExecutionStrategy.SERIAL)
                                .build())
                .build(),
            GraphNode.builder()
                .id("install")
                .name("install")
                .type(StateType.COMMAND.name())
                .properties(ImmutableMap.<String, Object>builder().put("command", "install").build())
                .build())
        .addLinks(
            aLink().withId("l1").withFrom("RepeatByServices").withTo("RepeatByInstances").withType("repeat").build())
        .addLinks(aLink().withId("l2").withFrom("RepeatByInstances").withTo("install").withType("repeat").build())
        .build();
  }

  private GraphNode getGraphNode(String repeatByServices, ExecutionStrategy parallel) {
    return GraphNode.builder()
        .id(repeatByServices)
        .origin(true)
        .name(repeatByServices)
        .type(StateType.REPEAT.name())
        .properties(ImmutableMap.<String, Object>builder()
                        .put("repeatElementExpression", "${services()}")
                        .put("executionStrategy", parallel)
                        .build())
        .build();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldTriggerCanaryWorkflow() throws InterruptedException {
    Service service = addService("svc1");

    ServiceTemplate serviceTemplate = getServiceTemplate(service);

    SettingAttribute computeProvider = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute().withAppId(app.getUuid()).withValue(aPhysicalDataCenterConfig().build()).build());

    InfrastructureMapping infrastructureMapping =
        createInfraMappingService(serviceTemplate, computeProvider, "Name4", "host1");

    triggerWorkflow(app.getAppId(), env, service, infrastructureMapping);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldTriggerTemplateCanaryWorkflow() throws InterruptedException {
    Service service1 = addService("svc1");

    Service service2 = addService("svc2");

    ServiceTemplate serviceTemplate1 = getServiceTemplate(service1);

    ServiceTemplate serviceTemplate2 = getServiceTemplate(service2);

    SettingAttribute computeProvider = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute().withAppId(app.getUuid()).withValue(aPhysicalDataCenterConfig().build()).build());

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.save(PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                                              .withName("Name2")
                                              .withAppId(app.getUuid())
                                              .withAccountId(app.getAccountId())
                                              .withEnvId(env.getUuid())
                                              .withHostNames(Lists.newArrayList("host1"))
                                              .withServiceTemplateId(serviceTemplate1.getUuid())
                                              .withComputeProviderSettingId(computeProvider.getUuid())
                                              .withComputeProviderType(computeProvider.getValue().getType())
                                              .withDeploymentType(SSH.name())
                                              .withHostConnectionAttrs(AccessType.KEY.name())
                                              .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
                                              .build());

    InfrastructureMapping templateInfraMapping =
        createInfraMappingService(serviceTemplate2, computeProvider, "Name3", "host12");
    triggerTemplateWorkflow(app.getAppId(), env, service1, infrastructureMapping, service2, templateInfraMapping);
  }

  private ServiceTemplate getServiceTemplate(Service service) {
    return wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
  }

  /**
   * Trigger workflow.
   *
   * @param appId                 the app id
   * @param env                   the env
   * @param service
   * @param infrastructureMapping
   * @return the string
   * @throws InterruptedException the interrupted exception
   */
  public String triggerWorkflow(String appId, Environment env, Service service,
      InfrastructureMapping infrastructureMapping) throws InterruptedException {
    Workflow workflow = createWorkflow(appId, env, service, infrastructureMapping);
    ExecutionArgs executionArgs = new ExecutionArgs();

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    callback.await(ofSeconds(15));

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId, true);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);

    List<StateExecutionInstance> response =
        wingsPersistence
            .query(StateExecutionInstance.class,
                PageRequestBuilder.aPageRequest()
                    .addFilter(StateExecutionInstanceKeys.appId, EQ, appId)
                    .addFilter(StateExecutionInstanceKeys.executionUuid, EQ, execution.getUuid())
                    .addFilter(StateExecutionInstanceKeys.stateType, EQ, EMAIL.name())
                    .build())
            .getResponse();
    assertThat(response).isNotNull().isNotEmpty();
    List<ContextElement> elements = response.get(0)
                                        .getContextElements()
                                        .stream()
                                        .filter(contextElement
                                            -> contextElement.getElementType() == ContextElementType.PARAM
                                                && contextElement.getName() == PHASE_PARAM)
                                        .collect(toList());
    assertThat(elements).isNotNull().isNotEmpty();
    assertThat(elements.get(0)).isInstanceOf(PhaseElement.class);
    assertThat(((PhaseElement) elements.get(0)).getPhaseName()).isNotEmpty();
    return executionId;
  }
  /**
   * Trigger template workflow.
   *
   * @param appId                 the app id
   * @param env                   the env
   * @param service
   * @param infrastructureMapping
   * @param templateService
   *@param templateInfraMapping @return the string
   * @throws InterruptedException the interrupted exception
   */
  private String triggerTemplateWorkflow(String appId, Environment env, Service service,
      InfrastructureMapping infrastructureMapping, Service templateService, InfrastructureMapping templateInfraMapping)
      throws InterruptedException {
    Workflow workflow = createTemplateWorkflow(appId, env, service, infrastructureMapping);
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowVariables(
        ImmutableMap.of("Service", templateService.getUuid(), "ServiceInfra_SSH", templateInfraMapping.getUuid()));

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    callback.await(ofSeconds(15));

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId, true);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    return executionId;
  }
  private Workflow createWorkflow(
      String appId, Environment env, Service service, InfrastructureMapping infrastructureMapping) {
    Workflow orchestrationWorkflow =
        aWorkflow()
            .name(WORKFLOW_NAME)
            .appId(appId)
            .envId(env.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                       .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT).build())
                                       .addWorkflowPhase(aWorkflowPhase()
                                                             .name("Phase1")
                                                             .serviceId(service.getUuid())
                                                             .deploymentType(SSH)
                                                             .infraMappingId(infrastructureMapping.getUuid())
                                                             .build())
                                       .withPostDeploymentSteps(aPhaseStep(PhaseStepType.POST_DEPLOYMENT).build())
                                       .build())
            .build();

    Workflow orchestrationWorkflow2 = workflowService.createWorkflow(orchestrationWorkflow);
    assertThat(orchestrationWorkflow2).isNotNull().hasFieldOrProperty("uuid");
    assertThat(orchestrationWorkflow2.getOrchestrationWorkflow())
        .isNotNull()
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");

    Workflow orchestrationWorkflow3 =
        workflowService.readWorkflow(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid());
    assertThat(orchestrationWorkflow3).isNotNull();
    assertThat(orchestrationWorkflow3.getOrchestrationWorkflow())
        .isNotNull()
        .isInstanceOf(CanaryOrchestrationWorkflow.class);
    assertThat(((CanaryOrchestrationWorkflow) orchestrationWorkflow3.getOrchestrationWorkflow()).getWorkflowPhases())
        .isNotNull()
        .hasSize(1);

    WorkflowPhase workflowPhase =
        ((CanaryOrchestrationWorkflow) orchestrationWorkflow3.getOrchestrationWorkflow()).getWorkflowPhases().get(0);
    PhaseStep deployPhaseStep = workflowPhase.getPhaseSteps()
                                    .stream()
                                    .filter(ps -> ps.getPhaseStepType() == PhaseStepType.DEPLOY_SERVICE)
                                    .collect(toList())
                                    .get(0);

    deployPhaseStep.getSteps().add(
        GraphNode.builder()
            .type("EMAIL")
            .name("email")
            .properties(ImmutableMap.<String, Object>builder().put("toAddress", "a@b.com").build())
            .build());

    workflowService.updateWorkflowPhase(
        orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid(), workflowPhase);

    Workflow orchestrationWorkflow4 =
        workflowService.readWorkflow(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid());

    assertThat(orchestrationWorkflow4).isNotNull();
    assertThat(orchestrationWorkflow4.getOrchestrationWorkflow())
        .isNotNull()
        .isInstanceOf(CanaryOrchestrationWorkflow.class);

    logger.info("Graph Json : \n {}",
        JsonUtils.asJson(((CanaryOrchestrationWorkflow) orchestrationWorkflow4.getOrchestrationWorkflow()).getGraph()));

    return orchestrationWorkflow4;
  }

  private Workflow createTemplateWorkflow(
      String appId, Environment env, Service service, InfrastructureMapping infrastructureMapping) {
    TemplateExpression infraExpression =
        TemplateExpression.builder()
            .fieldName("infraMappingId")
            .expression("${ServiceInfra_SSH}")
            .metadata(new HashMap<>(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING")))
            .build();

    TemplateExpression serviceExpression = TemplateExpression.builder()
                                               .fieldName("serviceId")
                                               .expression("${Service}")
                                               .metadata(new HashMap<>(ImmutableMap.of("entityType", "SERVICE")))
                                               .build();

    Workflow orchestrationWorkflow =
        aWorkflow()
            .name(WORKFLOW_NAME)
            .appId(appId)
            .envId(env.getUuid())
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .name("Phase1")
                                          .serviceId(service.getUuid())
                                          .deploymentType(SSH)
                                          .infraMappingId(infrastructureMapping.getUuid())
                                          .templateExpressions(asList(infraExpression, serviceExpression))
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(PhaseStepType.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow orchestrationWorkflow2 = workflowService.createWorkflow(orchestrationWorkflow);
    assertThat(orchestrationWorkflow2).isNotNull().hasFieldOrProperty("uuid");
    OrchestrationWorkflow orchestrationWorkflow1 = orchestrationWorkflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow1)
        .isNotNull()
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");
    assertThat(orchestrationWorkflow2.getTemplatizedServiceIds()).isNotNull().contains(service.getUuid());
    assertThat(orchestrationWorkflow1.getTemplatizedInfraMappingIds())
        .isNotNull()
        .contains(infrastructureMapping.getUuid());
    assertThat(orchestrationWorkflow1).extracting("userVariables").isNotNull();
    assertThat(
        orchestrationWorkflow1.getUserVariables().stream().anyMatch(variable -> variable.getName().equals("Service")))
        .isTrue();
    assertThat(orchestrationWorkflow1)
        .isNotNull()
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");

    Workflow orchestrationWorkflow3 =
        workflowService.readWorkflow(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid());
    assertThat(orchestrationWorkflow3).isNotNull();
    assertThat(orchestrationWorkflow3.getOrchestrationWorkflow())
        .isNotNull()
        .isInstanceOf(CanaryOrchestrationWorkflow.class);
    assertThat(((CanaryOrchestrationWorkflow) orchestrationWorkflow3.getOrchestrationWorkflow()).getWorkflowPhases())
        .isNotNull()
        .hasSize(1);

    WorkflowPhase workflowPhase =
        ((CanaryOrchestrationWorkflow) orchestrationWorkflow3.getOrchestrationWorkflow()).getWorkflowPhases().get(0);
    PhaseStep deployPhaseStep = workflowPhase.getPhaseSteps()
                                    .stream()
                                    .filter(ps -> ps.getPhaseStepType() == PhaseStepType.DEPLOY_SERVICE)
                                    .collect(toList())
                                    .get(0);

    deployPhaseStep.getSteps().add(
        GraphNode.builder()
            .type("EMAIL")
            .name("email")
            .properties(ImmutableMap.<String, Object>builder().put("toAddress", "a@b.com").build())
            .build());

    workflowService.updateWorkflowPhase(
        orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid(), workflowPhase);

    Workflow orchestrationWorkflow4 =
        workflowService.readWorkflow(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid());

    assertThat(orchestrationWorkflow4).isNotNull();
    assertThat(orchestrationWorkflow4.getOrchestrationWorkflow())
        .isNotNull()
        .isInstanceOf(CanaryOrchestrationWorkflow.class);

    logger.info("Graph Json : \n {}",
        JsonUtils.asJson(((CanaryOrchestrationWorkflow) orchestrationWorkflow4.getOrchestrationWorkflow()).getGraph()));

    return orchestrationWorkflow4;
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldObtainNoLastGoodDeployedArtifacts() {
    String appId = app.getUuid();
    Workflow workflow = createExecutableWorkflow(appId, env, "workflow1");
    List<Artifact> artifacts =
        workflowExecutionService.obtainLastGoodDeployedArtifacts(workflow.getAppId(), workflow.getUuid());
    assertThat(artifacts).isEmpty();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldObtainLastGoodDeployedArtifacts() throws InterruptedException {
    String appId = app.getUuid();
    Workflow workflow = createExecutableWorkflow(appId, env, "workflow1");
    WorkflowExecution workflowExecution = triggerWorkflow(workflow, env);
    assertThat(workflowExecution).isNotNull().hasFieldOrPropertyWithValue("releaseNo", "1");
    List<Artifact> artifacts =
        workflowExecutionService.obtainLastGoodDeployedArtifacts(workflow.getAppId(), workflow.getUuid());
    assertThat(artifacts).isNotEmpty();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetDeploymentMetadata() {
    String appId = app.getUuid();
    Service service = addService("svc1");

    ServiceTemplate serviceTemplate = getServiceTemplate(service);

    SettingAttribute computeProvider = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute().withAppId(app.getUuid()).withValue(aPhysicalDataCenterConfig().build()).build());

    InfrastructureMapping infrastructureMapping =
        createInfraMappingService(serviceTemplate, computeProvider, "Name4", "host1");

    Workflow workflow = createWorkflow(appId, env, service, infrastructureMapping);

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setOrchestrationId(workflow.getUuid());
    executionArgs.setWorkflowType(workflow.getWorkflowType());
    final DeploymentMetadata deploymentMetadata =
        workflowExecutionService.fetchDeploymentMetadata(workflow.getAppId(), executionArgs);
    assertThat(deploymentMetadata).isNotNull();
    assertThat(deploymentMetadata.getArtifactRequiredServiceIds()).isEmpty();
  }

  private InfrastructureMapping createInfraMappingService(
      ServiceTemplate serviceTemplate, SettingAttribute computeProvider, String name4, String host1) {
    return infrastructureMappingService.save(PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                                                 .withName(name4)
                                                 .withAppId(app.getUuid())
                                                 .withEnvId(env.getUuid())
                                                 .withAccountId(app.getAccountId())
                                                 .withHostNames(Lists.newArrayList(host1))
                                                 .withServiceTemplateId(serviceTemplate.getUuid())
                                                 .withComputeProviderSettingId(computeProvider.getUuid())
                                                 .withComputeProviderType(computeProvider.getValue().getType())
                                                 .withDeploymentType(SSH.name())
                                                 .withHostConnectionAttrs(AccessType.KEY.name())
                                                 .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
                                                 .build());
  }

  private InfrastructureMapping createContainerInfraMappingService(
      ServiceTemplate serviceTemplate, SettingAttribute computeProvider, String serviceId) {
    String infraMappingId =
        wingsPersistence.save(DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping()
                                  .withClusterName("testClusterName")
                                  .withAccountId(app.getAccountId())
                                  .withAppId(app.getUuid())
                                  .withServiceId(serviceId)
                                  .withNamespace("default")
                                  .withEnvId(env.getUuid())
                                  .withServiceTemplateId(serviceTemplate.getUuid())
                                  .withComputeProviderSettingId(computeProvider.getUuid())
                                  .withComputeProviderType(computeProvider.getValue().getType())
                                  .withDeploymentType(DeploymentType.KUBERNETES.name())
                                  .withInfraMappingType(KUBERNETES.name())
                                  .build());

    return wingsPersistence.get(InfrastructureMapping.class, infraMappingId);
  }

  private Service addService(String svc1) {
    return wingsPersistence.saveAndGet(Service.class,
        Service.builder()
            .uuid(generateUuid())
            .name(svc1)
            .appId(app.getUuid())
            .artifactType(ArtifactType.DOCKER)
            .build());
  }

  @Test
  @Owner(developers = {SRINIVAS})
  @Category(UnitTests.class)
  public void shouldListWaitingOnDeployments() {
    List<WorkflowExecution> waitingOnDeployments = getWorkflowExecutions(false);
    assertThat(waitingOnDeployments).isNotEmpty().hasSize(2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testShouldNotQueueDeployment() {
    when(featureFlagService.isEnabled(eq(INFRA_MAPPING_REFACTOR), any())).thenReturn(true, true, true, true);
    List<WorkflowExecution> waitingOnDeployments = getWorkflowExecutions(true);
    assertThat(waitingOnDeployments).isNotEmpty().hasSize(2);
    List<ExecutionStatus> executionStatuses =
        waitingOnDeployments.stream().map(WorkflowExecution::getStatus).collect(toList());
    assertThat(executionStatuses).isNotEmpty().hasSize(2);
    assertThat(executionStatuses.get(0)).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(executionStatuses.get(1)).isEqualTo(ExecutionStatus.RUNNING);
  }

  private List<WorkflowExecution> getWorkflowExecutions(boolean withConcurrencyStrategy) {
    String appId = app.getUuid();
    Workflow workflow = withConcurrencyStrategy ? createExecutableWorkflowWithThrottling(appId, env, "workflow1")
                                                : createExecutableWorkflow(appId, env, "workflow1");
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(
        singletonList(Artifact.Builder.anArtifact().withAppId(APP_ID).withUuid(ARTIFACT_ID).build()));

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution firstExecution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);

    assertThat(firstExecution).isNotNull();

    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    assertThat(execution).isNotNull();

    return workflowExecutionService.listWaitingOnDeployments(appId, execution.getUuid());
  }

  @Test
  @Owner(developers = {SRINIVAS})
  @Category(UnitTests.class)
  public void shouldFetchWorkflowExecutionStartTs() throws Exception {
    String appId = app.getUuid();
    Workflow workflow = createExecutableWorkflow(appId, env, "workflow1");
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(asList(Artifact.Builder.anArtifact().withAppId(APP_ID).withUuid(ARTIFACT_ID).build()));

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    callback.await(ofSeconds(15));

    assertThat(execution).isNotNull();

    assertThat(workflowExecutionService.fetchWorkflowExecutionStartTs(execution.getAppId(), execution.getUuid()))
        .isNotNull();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldPopulateArtifactsAndServices() {
    String serviceId1 = SERVICE_ID + "_1";
    String artifactId1 = ARTIFACT_ID + "_1";
    String artifactStreamId1 = ARTIFACT_STREAM_ID + "_1";
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(
        asList(anArtifact().withUuid(ARTIFACT_ID).build(), anArtifact().withUuid(artifactId1).build()));
    when(artifactService.listByIds(any(), any()))
        .thenReturn(asList(
            anArtifact().withUuid(ARTIFACT_ID).withArtifactStreamId(ARTIFACT_STREAM_ID).withDisplayName("art").build(),
            anArtifact()
                .withUuid(artifactId1)
                .withArtifactStreamId(artifactStreamId1)
                .withDisplayName("art1")
                .build()));

    WorkflowExecution workflowExecution =
        WorkflowExecution.builder().serviceIds(asList(SERVICE_ID, serviceId1)).build();
    when(artifactStreamServiceBindingService.listArtifactStreamIds(SERVICE_ID))
        .thenReturn(singletonList(ARTIFACT_STREAM_ID));
    when(artifactStreamServiceBindingService.listArtifactStreamIds(serviceId1))
        .thenReturn(singletonList(artifactStreamId1));
    when(artifactStreamServiceBindingService.listServices(ARTIFACT_STREAM_ID))
        .thenReturn(singletonList(Service.builder().uuid(SERVICE_ID).name("s").build()));
    when(artifactStreamServiceBindingService.listServices(artifactStreamId1))
        .thenReturn(singletonList(Service.builder().uuid(serviceId1).name("s1").build()));

    WorkflowStandardParams stdParams = aWorkflowStandardParams().build();
    Set<String> keywords = new HashSet<>();
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .populateArtifactsAndServices(workflowExecution, stdParams, keywords, executionArgs, ACCOUNT_ID);

    Function<List<Artifact>, Boolean> checkArtifacts = artifacts
        -> EmptyPredicate.isNotEmpty(artifacts) && artifacts.size() == 2
        && artifacts.get(0).getUuid().equals(ARTIFACT_ID) && artifacts.get(1).getUuid().equals(artifactId1);
    assertThat(checkArtifacts.apply(workflowExecution.getArtifacts())).isTrue();
    assertThat(checkArtifacts.apply(executionArgs.getArtifacts())).isTrue();
    assertThat(keywords).contains("s", "s1");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldPopulateArtifactsAndServicesNoArtifactIds() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(Collections.singletonList(anArtifact().build()));
    WorkflowExecution workflowExecution = WorkflowExecution.builder().build();
    WorkflowStandardParams stdParams = aWorkflowStandardParams().build();
    Set<String> keywords = new HashSet<>();
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .populateArtifactsAndServices(workflowExecution, stdParams, keywords, executionArgs, ACCOUNT_ID);
    assertThat(workflowExecution.getArtifacts()).isNullOrEmpty();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotPopulateArtifactsAndServicesWithInvalidArtifacts() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(Collections.singletonList(anArtifact().withUuid(ARTIFACT_ID).build()));
    when(artifactService.listByIds(any(), any())).thenReturn(Collections.emptyList());
    WorkflowExecution workflowExecution = WorkflowExecution.builder().build();
    WorkflowStandardParams stdParams = aWorkflowStandardParams().build();
    Set<String> keywords = new HashSet<>();
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .populateArtifactsAndServices(workflowExecution, stdParams, keywords, executionArgs, ACCOUNT_ID);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldPopulateArtifactsAndServicesWithArtifactStreamRefactorBasic() {
    // This is just to test that populateArtifacts function is called for feature-flag on.
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(true);
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifactVariables(Collections.emptyList());
    WorkflowExecution workflowExecution = WorkflowExecution.builder().build();
    WorkflowStandardParams stdParams = aWorkflowStandardParams().build();
    Set<String> keywords = new HashSet<>();
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .populateArtifactsAndServices(workflowExecution, stdParams, keywords, executionArgs, ACCOUNT_ID);
    verify(multiArtifactWorkflowExecutionServiceHelper).filterArtifactsForExecution(any(), any(), any());
    assertThat(workflowExecution.getArtifacts()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchConcurrentExecutions() {
    int count = 0;
    String firstExecutionId = null;
    List<ResourceConstraintInstance> resourceConstraintInstances = new ArrayList<>();
    List<WorkflowExecution> executions = getWorkflowExecutions(true);
    for (WorkflowExecution execution : executions) {
      resourceConstraintInstances.add(ResourceConstraintInstance.builder()
                                          .releaseEntityId(execution.getUuid())
                                          .releaseEntityType(HoldingScope.WORKFLOW.name())
                                          .resourceUnit(INFRA_MAPPING_ID)
                                          .state(count == 0 ? State.ACTIVE.name() : State.BLOCKED.name())
                                          .build());
      if (count == 0) {
        firstExecutionId = execution.getUuid();
      }
      count++;
    }
    when(resourceConstraintService.fetchResourceConstraintInstancesForUnitAndEntityType(any(), any(), any(), any()))
        .thenReturn(resourceConstraintInstances);
    when(resourceConstraintService.getByName(account.getUuid(), RESOURCE_CONSTRAINT_NAME))
        .thenReturn(ResourceConstraint.builder()
                        .name(RESOURCE_CONSTRAINT_NAME)
                        .accountId(account.getUuid())
                        .capacity(1)
                        .strategy(Strategy.FIFO)
                        .build());
    ConcurrentExecutionResponse response = workflowExecutionService.fetchConcurrentExecutions(
        app.getUuid(), firstExecutionId, RESOURCE_CONSTRAINT_NAME, INFRA_MAPPING_ID);
    assertThat(response).isNotNull();
    assertThat(response.getUnitType()).isEqualTo(UnitType.INFRA);
    assertThat(response.getExecutions()).isNotNull().hasSize(2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testExtractInfrastructureDetails() {
    Service service = addService("Service");
    ServiceTemplate serviceTemplate = getServiceTemplate(service);

    String computeProviderId = wingsPersistence.save(
        aSettingAttribute().withAppId(app.getUuid()).withValue(aPhysicalDataCenterConfig().build()).build());

    SettingAttribute computeProvider =
        wingsPersistence.getWithAppId(SettingAttribute.class, app.getUuid(), computeProviderId);

    InfrastructureMapping infrastructureMapping =
        createInfraMappingService(serviceTemplate, computeProvider, "Name4", "host1");

    WorkflowExecution execution = WorkflowExecution.builder()
                                      .appId(app.getUuid())
                                      .infraDefinitionIds(singletonList(INFRA_DEFINITION_ID))
                                      .infraMappingIds(singletonList(infrastructureMapping.getUuid()))
                                      .serviceIds(singletonList(service.getUuid()))
                                      .build();
    Map<String, Object> infraMap =
        workflowExecutionService.extractServiceInfrastructureDetails(app.getUuid(), execution);
    assertThat(infraMap).isNotNull();
    assertThat(infraMap.containsKey("Service")).isTrue();
    assertThat(infraMap.get("Service")).isEqualTo("Service");
    assertThat(infraMap.containsKey("CloudProvider")).isTrue();
    assertThat(infraMap.get("CloudProvider")).isEqualTo("PHYSICAL_DATA_CENTER");

    computeProviderId = wingsPersistence.save(
        aSettingAttribute().withAppId(app.getUuid()).withValue(KubernetesClusterConfig.builder().build()).build());
    computeProvider = wingsPersistence.getWithAppId(SettingAttribute.class, app.getUuid(), computeProviderId);

    execution.setInfraMappingIds(singletonList(
        createContainerInfraMappingService(serviceTemplate, computeProvider, service.getUuid()).getUuid()));
    infraMap = workflowExecutionService.extractServiceInfrastructureDetails(app.getUuid(), execution);
    assertThat(infraMap).isNotNull();
    assertThat(infraMap.get("ClusterName")).isEqualTo("testClusterName");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void emptyExecutionHostsWithTargetEnabledShouldThrowException() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setHosts(Collections.emptyList());
    executionArgs.setTargetToSpecificHosts(true);
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);

    assertThatThrownBy(
        () -> ((WorkflowExecutionServiceImpl) workflowExecutionService).validateExecutionArgsHosts(executionArgs, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Host list can't be empty when Target To Specific Hosts option is enabled");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void executionHostsShouldNotBeSetForPipelines() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setTargetToSpecificHosts(true);
    executionArgs.setHosts(Arrays.asList("host1", "host2"));
    executionArgs.setWorkflowType(WorkflowType.PIPELINE);

    assertThatThrownBy(
        () -> ((WorkflowExecutionServiceImpl) workflowExecutionService).validateExecutionArgsHosts(executionArgs, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("pipeline");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void executionHostsShouldNotBeSetForTriggers() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setTargetToSpecificHosts(true);
    executionArgs.setHosts(Arrays.asList("host1", "host2"));
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    Trigger trigger = Trigger.builder().build();

    assertThatThrownBy(()
                           -> ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                  .validateExecutionArgsHosts(executionArgs, trigger))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("trigger");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void executionHostsShouldNotBeSetForNonSshDeploymentType() {
    List<String> hosts = Arrays.asList("host1", "host2");
    WorkflowExecution workflowExecution =
        WorkflowExecution.builder().serviceIds(Collections.singletonList("serviceId")).build();
    Workflow workflow = WorkflowBuilder.aWorkflow().build();
    workflow.setDeploymentTypes(Collections.singletonList(DeploymentType.KUBERNETES));

    assertThatThrownBy(()
                           -> ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                  .validateExecutionArgsHosts(hosts, workflowExecution, workflow))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Execution Hosts only supported for SSH deployment type");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void executionHostsShouldNotBeSetForMultiServicesInSingleWorkflow() {
    List<String> hosts = Arrays.asList("host1", "host2");
    List<String> servicesIds = Arrays.asList("serviceId1", "serviceId2");
    WorkflowExecution workflowExecution = WorkflowExecution.builder().serviceIds(servicesIds).build();

    assertThatThrownBy(()
                           -> ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                  .validateExecutionArgsHosts(hosts, workflowExecution, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("single service");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void targetHostsOptionDisabledShouldPassValidation() {
    ExecutionArgs executionArgs = spy(new ExecutionArgs());

    ((WorkflowExecutionServiceImpl) workflowExecutionService).validateExecutionArgsHosts(executionArgs, null);

    verify(executionArgs, times(1)).isTargetToSpecificHosts();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldTrimNonEmptyList() {
    WorkflowExecutionServiceImpl workflowExecutionService =
        (WorkflowExecutionServiceImpl) this.workflowExecutionService;
    List<String> trimmedList = workflowExecutionService.trimExecutionArgsHosts(Collections.singletonList(" abc "));

    assertThat(trimmedList).isEqualTo(Collections.singletonList("abc"));
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void trimShouldReturnEmptyForEmptyList() {
    WorkflowExecutionServiceImpl workflowExecutionService =
        (WorkflowExecutionServiceImpl) this.workflowExecutionService;
    List<String> trimmedList = workflowExecutionService.trimExecutionArgsHosts(Collections.emptyList());

    assertThat(trimmedList).isEqualTo(Collections.emptyList());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void trimShouldReturnEmptyForNullList() {
    WorkflowExecutionServiceImpl workflowExecutionService =
        (WorkflowExecutionServiceImpl) this.workflowExecutionService;
    List<String> trimmedList = workflowExecutionService.trimExecutionArgsHosts(null);

    assertThat(trimmedList).isEqualTo(Collections.emptyList());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testFetchWorkflowExecutionsForResourceConstraint() {
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < ids.size(); ++i) {
      String uuid = "_" + random.nextInt(1000);
      if (ids.contains(uuid)) {
        continue;
      }
      final WorkflowExecution workflowExecution = WorkflowExecution.builder().uuid(uuid).appId(APP_ID).build();
      ids.add(workflowExecution.getUuid());
      wingsPersistence.save(workflowExecution);
    }

    Collections.sort(ids);

    final List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.fetchWorkflowExecutionsForResourceConstraint(APP_ID, ids);

    List<String> wIds = workflowExecutions.stream().map(WorkflowExecution::getUuid).collect(toList());
    assertThat(wIds).isEqualTo(ids);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetStateExecutionDataInfraMapping() {
    String executionUuid = generateUuid();
    String displayName = generateUuid();
    String serviceId = generateUuid();
    String infraMappingId = generateUuid();
    String infraDefinitionId = generateUuid();

    final StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance()
            .appId(app.getUuid())
            .executionUuid(executionUuid)
            .stateType(StateType.PHASE_STEP.name())
            .displayName(displayName)
            .contextElement(PhaseElement.builder()
                                .infraMappingId(infraMappingId)
                                .infraDefinitionId(infraDefinitionId)
                                .serviceElement(ServiceElement.builder().uuid(serviceId).build())
                                .build())
            .build();

    wingsPersistence.save(stateExecutionInstance);

    List<StateExecutionInstance> stateExecutionData = workflowExecutionService.getStateExecutionData(
        app.getUuid(), executionUuid, serviceId, infraMappingId, Optional.empty(), StateType.PHASE_STEP, displayName);
    assertThat(stateExecutionData.size()).isEqualTo(1);

    stateExecutionData = workflowExecutionService.getStateExecutionData(app.getUuid(), executionUuid, serviceId, null,
        Optional.of(infraDefinitionId), StateType.PHASE_STEP, displayName);
    assertThat(stateExecutionData.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void getDeploymentTags() {
    WorkflowExecution workflowExecution = WorkflowExecution.builder()
                                              .accountId(ACCOUNT_ID)
                                              .tags(asList(NameValuePair.builder().name("key1").value("value1").build(),
                                                  NameValuePair.builder().name("label").value("").build()))
                                              .build();
    when(featureFlagService.isEnabled(eq(DEPLOYMENT_TAGS), eq(ACCOUNT_ID))).thenReturn(true, true);
    Map<String, String> deploymentTags =
        workflowExecutionService.getDeploymentTags(ACCOUNT_ID, workflowExecution.getTags());
    assertThat(deploymentTags).isNotEmpty();
    assertThat(deploymentTags.entrySet())
        .extracting(Map.Entry::getKey, Map.Entry::getValue)
        .contains(tuple("key1", "value1"), tuple("label", ""));

    workflowExecution = WorkflowExecution.builder().accountId(ACCOUNT_ID).build();
    deploymentTags = workflowExecutionService.getDeploymentTags(ACCOUNT_ID, workflowExecution.getTags());
    assertThat(deploymentTags).isNull();

    when(featureFlagService.isEnabled(eq(DEPLOYMENT_TAGS), eq(ACCOUNT_ID))).thenReturn(false);
    deploymentTags = workflowExecutionService.getDeploymentTags(ACCOUNT_ID, workflowExecution.getTags());
    assertThat(deploymentTags).isNull();
  }
}
