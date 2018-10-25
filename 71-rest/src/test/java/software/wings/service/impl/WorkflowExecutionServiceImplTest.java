package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.threading.Puller.pullFor;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import io.harness.threading.Puller;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.WorkflowElement;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
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
import software.wings.beans.LicenseInfo;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.scheduler.JobScheduler;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyEventListener;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The type Workflow service impl test.
 *
 * @author Rishi
 */
@Listeners({NotifyEventListener.class, ExecutionEventListener.class})
public class WorkflowExecutionServiceImplTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionServiceImplTest.class);
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Mock private JobScheduler jobScheduler;
  @Mock private FeatureFlagService featureFlagService;
  @Inject @InjectMocks private AccountService accountService;
  @Inject @InjectMocks private WorkflowExecutionService workflowExecutionService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @Mock private ArtifactService artifactService;

  @Mock private StaticConfiguration staticConfiguration;
  @Inject private ServiceInstanceService serviceInstanceService;

  @Inject @Named("waitStateResumer") private ScheduledExecutorService executorService;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @InjectMocks @Inject private Injector injector;
  @Rule public ExpectedException thrown = ExpectedException.none();

  private Account account;
  private Application app;
  private Environment env;
  private Artifact artifact;
  private PageResponse<Artifact> artifactPageResponse;

  /*
   * Should trigger simple workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Before
  public void setup() {
    when(jobScheduler.deleteJob(any(), any())).thenReturn(false);
    when(jobScheduler.scheduleJob(any(), any())).thenReturn(null);
    account = accountService.save(
        Account.Builder.anAccount().withCompanyName(COMPANY_NAME).withAccountName(ACCOUNT_NAME).build());
    app = wingsPersistence.saveAndGet(
        Application.class, anApplication().withName(APP_NAME).withAccountId(account.getUuid()).build());
    env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    artifact = anArtifact()
                   .withAppId(app.getAppId())
                   .withUuid(generateUuid())
                   .withDisplayName(ARTIFACT_NAME)
                   .withServiceIds(new ArrayList<>())
                   .build();
    artifactPageResponse = aPageResponse().withResponse(asList(artifact)).build();
    when(artifactService.list(any(PageRequest.class), eq(false))).thenReturn(artifactPageResponse);
  }

  @Test
  @Ignore
  public void shouldTriggerSimpleWorkflow() throws InterruptedException {
    Graph graph =
        aGraph()
            .addNodes(aGraphNode()
                          .withId("n1")
                          .withOrigin(true)
                          .withName("RepeatByInstances")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances()}")
                          .addProperty("executionStrategyExpression", "${SIMPLE_WORKFLOW_REPEAT_STRATEGY}")
                          .build(),
                aGraphNode()
                    .withId("n2")
                    .withName("email")
                    .withType(StateType.EMAIL.name())
                    .addProperty("toAddress", "a@b.com")
                    .addProperty("subject", "commandName : ${SIMPLE_WORKFLOW_COMMAND_NAME}")
                    .addProperty("body",
                        "service:${service.name}, serviceTemplate:${serviceTemplate.name}, host:${host.name}, instance:${instance.name}")
                    .build())
            .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("repeat").build())
            .build();

    when(staticConfiguration.defaultSimpleWorkflow()).thenReturn(graph);

    Host applicationHost1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host applicationHost2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(applicationHost1).build());
    ServiceInstance inst2 = serviceInstanceService.save(builder.withHost(applicationHost2).build());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setServiceInstances(Lists.newArrayList(inst1, inst2));
    executionArgs.setExecutionStrategy(ExecutionStrategy.SERIAL);
    executionArgs.setCommandName("START");
    executionArgs.setWorkflowType(WorkflowType.SIMPLE);
    executionArgs.setServiceId(service.getUuid());

    WorkflowServiceImpl impl = (WorkflowServiceImpl) workflowService;
    impl.setStaticConfiguration(staticConfiguration);

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecutionServiceImpl workflowExecutionServiceImpl = (WorkflowExecutionServiceImpl) workflowExecutionService;
    WorkflowExecution workflowExecution =
        workflowExecutionServiceImpl.triggerEnvExecution(app.getUuid(), env.getUuid(), executionArgs, callback, null);
    callback.await(ofSeconds(15));

    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getUuid()).isNotNull();

    WorkflowExecution workflowExecution2 =
        workflowExecutionService.getExecutionDetails(app.getUuid(), workflowExecution.getUuid(), true, emptySet());
    assertThat(workflowExecution2)
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getAppId, WorkflowExecution::getStateMachineId,
            WorkflowExecution::getWorkflowId)
        .containsExactly(workflowExecution.getUuid(), app.getUuid(), workflowExecution.getStateMachineId(),
            workflowExecution.getWorkflowId());
    assertThat(workflowExecution2.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    assertThat(workflowExecution2.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "RepeatByInstances")
        .hasFieldOrPropertyWithValue("type", "REPEAT")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(workflowExecution2.getExecutionNode().getGroup()).isNotNull();
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("host1", "host2");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("type")
        .contains("ELEMENT", "ELEMENT");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("name")
        .contains("email", "email");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("type")
        .contains("EMAIL", "EMAIL");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("status")
        .contains("SUCCESS", "SUCCESS");

    assertThat(workflowExecution2.getExecutionNode())
        .hasFieldOrProperty("elementStatusSummary")
        .hasFieldOrProperty("instanceStatusSummary");
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary()).isNotNull().hasSize(2);
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary().get(0))
        .isNotNull()
        .extracting("instancesCount", "status")
        .containsExactly(1, ExecutionStatus.SUCCESS);
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary()).isNotNull().hasSize(2);
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary().get(0))
        .isNotNull()
        .extracting("startTs", "endTs")
        .doesNotContainNull();
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary().get(0))
        .extracting("contextElement")
        .doesNotContainNull()
        .extracting("elementType")
        .hasSize(1)
        .containsExactly(ContextElementType.INSTANCE);
  }

  /**
   * Should trigger simple workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldRenderSimpleWorkflow() throws InterruptedException {
    Graph graph = aGraph()
                      .addNodes(aGraphNode()
                                    .withId("n1")
                                    .withOrigin(true)
                                    .withName("RepeatByInstances")
                                    .withType(StateType.REPEAT.name())
                                    .addProperty("repeatElementExpression", "${instances()}")
                                    .addProperty("executionStrategyExpression", "${SIMPLE_WORKFLOW_REPEAT_STRATEGY}")
                                    .build(),
                          aGraphNode()
                              .withId("n2")
                              .withName("stop")
                              .withType(StateType.COMMAND.name())
                              .addProperty("commandName", "${SIMPLE_WORKFLOW_COMMAND_NAME}")
                              .build())
                      .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("repeat").build())
                      .build();

    when(staticConfiguration.defaultSimpleWorkflow()).thenReturn(graph);

    Host applicationHost1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host applicationHost2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(applicationHost1).build());
    ServiceInstance inst2 = serviceInstanceService.save(builder.withHost(applicationHost2).build());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setServiceInstances(Lists.newArrayList(inst1, inst2));
    executionArgs.setExecutionStrategy(ExecutionStrategy.PARALLEL);
    executionArgs.setCommandName("STOP");
    executionArgs.setWorkflowType(WorkflowType.SIMPLE);
    executionArgs.setServiceId(service.getUuid());

    WorkflowServiceImpl impl = (WorkflowServiceImpl) workflowService;
    impl.setStaticConfiguration(staticConfiguration);

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecutionServiceImpl workflowExecutionServiceImpl = (WorkflowExecutionServiceImpl) workflowExecutionService;
    WorkflowExecution workflowExecution =
        workflowExecutionServiceImpl.triggerEnvExecution(app.getUuid(), env.getUuid(), executionArgs, callback, null);
    callback.await(ofSeconds(15));

    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getUuid()).isNotNull();

    WorkflowExecution workflowExecution2 =
        workflowExecutionService.getExecutionDetails(app.getUuid(), workflowExecution.getUuid(), true, emptySet());
    assertThat(workflowExecution2)
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getAppId, WorkflowExecution::getStateMachineId,
            WorkflowExecution::getWorkflowId)
        .containsExactly(workflowExecution.getUuid(), app.getUuid(), workflowExecution.getStateMachineId(),
            workflowExecution.getWorkflowId());
    assertThat(workflowExecution2.getStatus()).isEqualTo(ExecutionStatus.FAILED);

    assertThat(workflowExecution2.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "RepeatByInstances")
        .hasFieldOrPropertyWithValue("type", "REPEAT")
        .hasFieldOrPropertyWithValue("status", "FAILED");
    assertThat(workflowExecution2.getExecutionNode().getGroup()).isNotNull();
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("host1", "host2");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("type")
        .contains("ELEMENT", "ELEMENT");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("name")
        .contains("STOP", "STOP");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("type")
        .contains("COMMAND", "COMMAND");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("status")
        .contains("FAILED", "FAILED");

    PageRequest<WorkflowExecution> pageRequest =
        aPageRequest().addFilter("appId", EQ, app.getUuid()).addFilter("uuid", EQ, workflowExecution.getUuid()).build();
    PageResponse<WorkflowExecution> res = workflowExecutionService.listExecutions(pageRequest, true);
    assertThat(res).isNotNull().hasSize(1).doesNotContainNull();

    workflowExecution2 = res.get(0);
    assertThat(workflowExecution2)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getAppId, WorkflowExecution::getStateMachineId,
            WorkflowExecution::getWorkflowId)
        .containsExactly(workflowExecution.getUuid(), app.getUuid(), workflowExecution.getStateMachineId(),
            workflowExecution.getWorkflowId());
    assertThat(workflowExecution2.getStatus()).isEqualTo(ExecutionStatus.FAILED); // password
  }

  /**
   * Should trigger complex workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Ignore
  public void shouldTriggerComplexWorkflow() throws InterruptedException {
    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host host2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service1 = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());
    Service service2 = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc2").appId(app.getUuid()).build());
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
            .addNodes(aGraphNode()
                          .withId("Repeat By Services")
                          .withOrigin(true)
                          .withName("Repeat By Services")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build(),
                aGraphNode()
                    .withId("RepeatByInstances")
                    .withName("RepeatByInstances")
                    .withType(StateType.REPEAT.name())
                    .addProperty("repeatElementExpression", "${instances}")
                    .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                    .build(),
                aGraphNode()
                    .withId("svcRepeatWait")
                    .withName("svcRepeatWait")
                    .withType(StateType.WAIT.name())
                    .addProperty("duration", 1)
                    .build(),
                aGraphNode()
                    .withId("instRepeatWait")
                    .withName("instRepeatWait")
                    .withType(StateType.WAIT.name())
                    .addProperty("duration", 1)
                    .build(),
                aGraphNode()
                    .withId("instSuccessWait")
                    .withName("instSuccessWait")
                    .withType(StateType.WAIT.name())
                    .addProperty("duration", 1)
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
            .withEnvId(env.getUuid())
            .withAppId(app.getUuid())
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
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
    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    assertThat(execution.getKeywords())
        .contains(workflow.getName().toLowerCase(), OrchestrationWorkflowType.CUSTOM.name().toLowerCase(),
            app.getName().toLowerCase(), env.getEnvironmentType().name().toLowerCase(),
            WorkflowType.ORCHESTRATION.name().toLowerCase());

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

    List<GraphNode> instRepeatElements = repeatInstance.stream()
                                             .map(GraphNode::getGroup)
                                             .flatMap(group -> group.getElements().stream())
                                             .collect(toList());
    assertThat(instRepeatElements).extracting("type").contains("ELEMENT", "ELEMENT", "ELEMENT", "ELEMENT");

    List<GraphNode> instRepeatWait = instRepeatElements.stream().map(GraphNode::getNext).collect(toList());
    assertThat(instRepeatWait)
        .isNotNull()
        .hasSize(4)
        .extracting("name")
        .contains("instRepeatWait", "instRepeatWait", "instRepeatWait", "instRepeatWait");
    assertThat(instRepeatWait).extracting("type").contains("WAIT", "WAIT", "WAIT", "WAIT");
  }

  /**
   * Trigger pipeline.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Ignore
  public void triggerPipeline() throws InterruptedException {
    Service service = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());

    Pipeline pipeline = constructPipeline(service);

    triggerPipeline(app.getUuid(), pipeline, service);
  }

  private Pipeline constructPipeline(Service service) {
    Host host = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host").build());

    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst = serviceInstanceService.save(builder.withHost(host).build());

    Graph graph =
        aGraph()
            .addNodes(aGraphNode()
                          .withId("Repeat By Services")
                          .withOrigin(true)
                          .withName("Repeat By Services")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build(),
                aGraphNode()
                    .withId("RepeatByInstances")
                    .withName("RepeatByInstances")
                    .withType(StateType.REPEAT.name())
                    .addProperty("repeatElementExpression", "${instances}")
                    .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                    .build(),
                aGraphNode()
                    .withId("svcRepeatWait")
                    .withName("svcRepeatWait")
                    .withType(StateType.WAIT.name())
                    .addProperty("duration", 1)
                    .build(),
                aGraphNode()
                    .withId("instRepeatWait")
                    .withName("instRepeatWait")
                    .withType(StateType.WAIT.name())
                    .addProperty("duration", 1)
                    .build(),
                aGraphNode()
                    .withId("instSuccessWait")
                    .withName("instSuccessWait")
                    .withType(StateType.WAIT.name())
                    .addProperty("duration", 1)
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
            .withEnvId(env.getUuid())
            .withAppId(app.getUuid())
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
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

    PageRequest<StateMachine> req = aPageRequest()
                                        .addFilter(StateMachine.APP_ID_KEY, EQ, app.getUuid())
                                        .addFilter(StateMachine.ORIGIN_ID_KEY, EQ, pipeline.getUuid())
                                        .build();
    PageResponse<StateMachine> res = workflowService.listStateMachines(req);

    assertThat(res).isNotNull().hasSize(1).doesNotContainNull();
    assertThat(res.get(0).getTransitions()).hasSize(0);
    return pipeline;
  }

  private WorkflowExecution triggerPipeline(String appId, Pipeline pipeline, Service service)
      throws InterruptedException {
    Artifact artifact = wingsPersistence.saveAndGet(Artifact.class,
        anArtifact()
            .withAppId(app.getUuid())
            .withDisplayName(ARTIFACT_NAME)
            .withServiceIds(asList(service.getUuid()))
            .build());
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
    execution = workflowExecutionService.getExecutionDetails(appId, executionId, true, emptySet());
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
  public void shouldTriggerWorkflow() throws InterruptedException {
    String appId = app.getUuid();
    triggerWorkflow(appId, env);
  }

  /**
   * Should trigger workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerWorkflowWithRelease() throws InterruptedException {
    String appId = app.getUuid();
    Workflow workflow = createExecutableWorkflow(appId, env);
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
        .containsExactly(workflowExecution2.getReleaseNo(), workflowExecution2.getDisplayName(),
            workflowExecution.getUuid(), workflowExecution.getDisplayName(), workflowExecution.getReleaseNo());
  }

  private WorkflowElement getWorkflowElement(String appId, WorkflowExecution workflowExecution) {
    StateExecutionInstance stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class,
        PageRequestBuilder.aPageRequest()
            .addFilter("appId", EQ, appId)
            .addFilter("executionUuid", EQ, workflowExecution.getUuid())
            .build());

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
  public void shouldGetNodeDetails() throws InterruptedException {
    String appId = app.getUuid();

    final WorkflowExecution triggerWorkflow = triggerWorkflow(appId, env);
    WorkflowExecution execution =
        workflowExecutionService.getExecutionDetails(appId, triggerWorkflow.getUuid(), true, emptySet());
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
  public void shouldUpdateFailedCount() throws InterruptedException {
    String appId = app.getUuid();
    triggerWorkflow(appId, env);
    WorkflowExecution workflowExecution = wingsPersistence.get(
        WorkflowExecution.class, aPageRequest().addFilter(WorkflowExecution.APP_ID_KEY, EQ, appId).build());
    workflowExecutionService.incrementFailed(workflowExecution.getAppId(), workflowExecution.getUuid(), 1);
    workflowExecution = wingsPersistence.get(
        WorkflowExecution.class, aPageRequest().addFilter(WorkflowExecution.APP_ID_KEY, EQ, appId).build());
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
  public WorkflowExecution triggerWorkflow(String appId, Environment env) throws InterruptedException {
    Workflow workflow = createExecutableWorkflow(appId, env);
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
    executionArgs.setArtifacts(asList(Artifact.Builder.anArtifact().withAppId(APP_ID).withUuid(ARTIFACT_ID).build()));

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    callback.await(ofSeconds(15));

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId, true, emptySet());
    assertThat(execution)
        .isNotNull()
        .hasFieldOrProperty("displayName")
        .hasFieldOrProperty("releaseNo")
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    return execution;
  }

  @Test
  public void shouldTriggerWorkflowFailForExpiredTrialLicense() throws InterruptedException {
    when(featureFlagService.isEnabled(FeatureName.TRIAL_SUPPORT, account.getUuid())).thenReturn(true);
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    licenseInfo.setExpiryTime(System.currentTimeMillis() + 5000);
    account.setLicenseInfo(licenseInfo);
    accountService.updateAccountLicense(account.getUuid(), licenseInfo, null, false);

    Thread.sleep(10000);
    Workflow workflow = createExecutableWorkflow(app.getUuid(), env);
    String appId = workflow.getAppId();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(asList(Artifact.Builder.anArtifact().withAppId(APP_ID).withUuid(ARTIFACT_ID).build()));

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    thrown.expect(WingsException.class);
    workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);

    // Scenario 2, update the license to be valid and test again.

    licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(System.currentTimeMillis() + 1000000);
    account.setLicenseInfo(licenseInfo);
    accountService.updateAccountLicense(account.getUuid(), licenseInfo, null, false);

    WorkflowExecution workflowExecution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    assertNotNull(workflowExecution);
  }

  @Test
  public void shouldTriggerPipelineFailForExpiredTrialLicense() throws InterruptedException {
    when(featureFlagService.isEnabled(FeatureName.TRIAL_SUPPORT, account.getUuid())).thenReturn(true);
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    licenseInfo.setExpiryTime(System.currentTimeMillis() + 5000);
    account.setLicenseInfo(licenseInfo);
    accountService.updateAccountLicense(account.getUuid(), licenseInfo, null, false);

    Thread.sleep(10000);
    Service service = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());

    Pipeline pipeline = constructPipeline(service);
    thrown.expect(WingsException.class);
    triggerPipeline(app.getUuid(), pipeline, service);

    // Scenario 2, update the license to be valid and test again.
    licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.TRIAL);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(System.currentTimeMillis() + 1000000);
    account.setLicenseInfo(licenseInfo);
    accountService.updateAccountLicense(account.getUuid(), licenseInfo, null, false);

    WorkflowExecution workflowExecution = triggerPipeline(app.getUuid(), pipeline, service);
    assertNotNull(workflowExecution);
  }

  /**
   * Trigger workflow.
   *
   * @param appId the app id
   * @param env   the env
   * @return the string
   * @throws InterruptedException the interrupted exception
   */
  public String triggerTemplateWorkflow(String appId, Environment env) throws InterruptedException {
    Workflow workflow = createExecutableWorkflow(appId, env);
    ExecutionArgs executionArgs = new ExecutionArgs();

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    callback.await(ofSeconds(15));

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId, true, emptySet());
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    return executionId;
  }

  private Workflow createExecutableWorkflow(String appId, Environment env) {
    Graph graph = aGraph()
                      .addNodes(aGraphNode()
                                    .withId("n1")
                                    .withName("wait")
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1l)
                                    .withOrigin(true)
                                    .build(),
                          aGraphNode()
                              .withId("n2")
                              .withName("email")
                              .withType(StateType.EMAIL.name())
                              .addProperty("toAddress", "a@b.com")
                              .addProperty("subject", "testing")
                              .build())
                      .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
                      .build();

    Workflow workflow =
        aWorkflow()
            .withEnvId(env.getUuid())
            .withAppId(appId)
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();
    return workflow;
  }

  /**
   * Should list workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldListWorkflow() throws InterruptedException {
    String appId = app.getUuid();

    triggerWorkflow(appId, env);

    // 2nd workflow
    createExecutableWorkflow(appId, env);
    PageRequest<Workflow> pageRequest = aPageRequest().addFilter(Workflow.APP_ID_KEY, EQ, appId).build();
    PageResponse<Workflow> res = workflowService.listWorkflows(pageRequest, null);

    assertThat(res).isNotNull().hasSize(2);
  }

  /**
   * Should pause and resume
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldPauseAndResumeState() throws InterruptedException {
    Graph graph = getAbortedGraph();

    Workflow workflow =
        aWorkflow()
            .withEnvId(env.getUuid())
            .withAppId(app.getUuid())
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
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

    pullFor(ofSeconds(10), () -> {
      final WorkflowExecution pull =
          workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
      return pull.getStatus() == ExecutionStatus.PAUSED;
    });

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());

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
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withExecutionUuid(executionId)
            .withStateExecutionInstanceId(execution.getExecutionNode().getNext().getId())
            .withExecutionInterruptType(ExecutionInterruptType.RESUME)
            .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
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
  @Ignore
  public void shouldPauseAllAndResumeAllState() throws InterruptedException {
    Service service1 = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());
    Service service2 = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc2").appId(app.getUuid()).build());

    Graph graph = getGraph();

    Workflow workflow =
        aWorkflow()
            .withEnvId(ENV_ID)
            .withAppId(app.getUuid())
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
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

    pullFor(ofSeconds(3), () -> {
      final WorkflowExecution pull =
          workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
      return pull.getStatus() == ExecutionStatus.RUNNING;
    });

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                                .withExecutionUuid(executionId)
                                                .withEnvId(env.getUuid())
                                                .build();
    executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    assertThat(executionInterrupt).isNotNull().hasFieldOrProperty("uuid");

    pullFor(ofSeconds(15), () -> {
      final WorkflowExecution pull =
          workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
      return pull.getStatus() == ExecutionStatus.PAUSED && pull.getExecutionNode().getGroup() != null;
    });

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());

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
                             .withAppId(app.getUuid())
                             .withExecutionInterruptType(ExecutionInterruptType.RESUME_ALL)
                             .withExecutionUuid(executionId)
                             .withEnvId(env.getUuid())
                             .build();
    executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    assertThat(executionInterrupt).isNotNull().hasFieldOrProperty("uuid");

    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
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
        .addNodes(aGraphNode()
                      .withId("RepeatByServices")
                      .withOrigin(true)
                      .withName("RepeatByServices")
                      .withType(StateType.REPEAT.name())
                      .addProperty("repeatElementExpression", "${services()}")
                      .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                      .build(),
            aGraphNode()
                .withId("wait1")
                .withName("wait1")
                .withType(StateType.WAIT.name())
                .addProperty("duration", 1)
                .build(),
            aGraphNode()
                .withId("wait2")
                .withName("wait2")
                .withType(StateType.WAIT.name())
                .addProperty("duration", 1)
                .build())
        .addLinks(aLink().withId("l1").withFrom("RepeatByServices").withTo("wait1").withType("repeat").build())
        .addLinks(aLink().withId("l2").withFrom("wait1").withTo("wait2").withType("success").build())
        .build();
  }

  /**
   * Should throw invalid argument for invalid workflow id.
   */
  @Test
  public void shouldThrowInvalidArgumentForInvalidWorkflowId() {
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.PAUSE)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(generateUuid())
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
  public void shouldAbortState() throws InterruptedException {
    Graph graph = getAbortedGraph();

    Workflow workflow =
        aWorkflow()
            .withEnvId(env.getUuid())
            .withAppId(app.getUuid())
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
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

    pullFor(ofSeconds(10), () -> {
      final WorkflowExecution pull =
          workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
      return pull.getStatus() == ExecutionStatus.PAUSED;
    });

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
    assertThat(execution).isNotNull().extracting("uuid", "status").containsExactly(executionId, ExecutionStatus.PAUSED);

    assertThat(execution.getExecutionNode()).isNotNull();

    assertThat(execution.getExecutionNode().getNext()).isNotNull();

    ExecutionInterrupt executionInterrupt =
        anExecutionInterrupt()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withExecutionUuid(executionId)
            .withStateExecutionInstanceId(execution.getExecutionNode().getNext().getId())
            .withExecutionInterruptType(ExecutionInterruptType.ABORT)
            .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.ABORTED);

    assertThat(execution.getExecutionNode()).isNotNull();
  }

  private Graph getAbortedGraph() {
    return aGraph()
        .addNodes(aGraphNode()
                      .withId("wait1")
                      .withOrigin(true)
                      .withName("wait1")
                      .withType(StateType.WAIT.name())
                      .addProperty("duration", 1)
                      .build(),
            aGraphNode()
                .withId("pause1")
                .withName("pause1")
                .withType(StateType.PAUSE.name())
                .addProperty("toAddress", "to1")
                .build(),
            aGraphNode()
                .withId("wait2")
                .withName("wait2")
                .withType(StateType.WAIT.name())
                .addProperty("duration", 1)
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
  public void shouldAbortAllStates() {
    Service service1 = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());
    Service service2 = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc2").appId(app.getUuid()).build());

    Graph graph = getGraph();

    Workflow workflow =
        aWorkflow()
            .withEnvId(env.getUuid())
            .withAppId(app.getUuid())
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
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

    pullFor(ofSeconds(5), () -> {
      final WorkflowExecution pull =
          workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
      return pull.getStatus() == ExecutionStatus.RUNNING;
    });

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.ABORT_ALL)
                                                .withExecutionUuid(executionId)
                                                .withEnvId(env.getUuid())
                                                .build();
    executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    assertThat(executionInterrupt).isNotNull().hasFieldOrProperty("uuid");

    pullFor(ofSeconds(15), () -> {
      final WorkflowExecution pull =
          workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
      return pull.getStatus() == ExecutionStatus.ABORTED;
    });

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());

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
  @Ignore
  public void shouldWaitOnError() throws InterruptedException {
    Host applicationHost1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host applicationHost2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(applicationHost1).build());
    ServiceInstance inst2 = serviceInstanceService.save(builder.withHost(applicationHost2).build());

    Graph graph =
        aGraph()
            .addNodes(aGraphNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build(),
                aGraphNode()
                    .withId("RepeatByInstances")
                    .withName("RepeatByInstances")
                    .withType(StateType.REPEAT.name())
                    .addProperty("repeatElementExpression", "${instances()}")
                    .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                    .build(),
                aGraphNode()
                    .withId("install")
                    .withName("install")
                    .withType(StateType.COMMAND.name())
                    .addProperty("command", "install")
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
            .withEnvId(env.getUuid())
            .withAppId(app.getUuid())
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
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

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
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
                                                .withAppId(app.getUuid())
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(executionId)
                                                .withStateExecutionInstanceId(installNode.getId())
                                                .withExecutionInterruptType(ExecutionInterruptType.MARK_SUCCESS)
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
                             .withAppId(app.getUuid())
                             .withEnvId(env.getUuid())
                             .withExecutionUuid(executionId)
                             .withStateExecutionInstanceId(installNode.getId())
                             .withExecutionInterruptType(ExecutionInterruptType.IGNORE)
                             .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
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
    Puller.pullFor(ofSeconds(10), () -> {
      WorkflowExecution execution =
          workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
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

    return installNodes(workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet()));
  }

  /**
   * Should retry on error
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Ignore
  public void shouldRetryOnError() throws InterruptedException {
    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host1").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(host1).build());

    Graph graph =
        aGraph()
            .addNodes(aGraphNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build(),
                aGraphNode()
                    .withId("RepeatByInstances")
                    .withName("RepeatByInstances")
                    .withType(StateType.REPEAT.name())
                    .addProperty("repeatElementExpression", "${instances()}")
                    .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                    .build(),
                aGraphNode()
                    .withId("install")
                    .withName("install")
                    .withType(StateType.COMMAND.name())
                    .addProperty("command", "install")
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
            .withEnvId(env.getUuid())
            .withAppId(app.getUuid())
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
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

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
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
                                                .withAppId(app.getUuid())
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(executionId)
                                                .withStateExecutionInstanceId(installNode.getId())
                                                .withExecutionInterruptType(ExecutionInterruptType.RETRY)
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
                             .withAppId(app.getUuid())
                             .withEnvId(env.getUuid())
                             .withExecutionUuid(executionId)
                             .withStateExecutionInstanceId(installNode.getId())
                             .withExecutionInterruptType(ExecutionInterruptType.MARK_SUCCESS)
                             .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await(ofSeconds(15));

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId, true, emptySet());
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

  @Test
  public void shouldTriggerCanaryWorkflow() throws InterruptedException {
    Service service = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());

    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    SettingAttribute computeProvider = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute().withAppId(app.getUuid()).withValue(aPhysicalDataCenterConfig().build()).build());

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.save(PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                                              .withName("Name4")
                                              .withAppId(app.getUuid())
                                              .withEnvId(env.getUuid())
                                              .withAccountId(app.getAccountId())
                                              .withHostNames(Lists.newArrayList("host1"))
                                              .withServiceTemplateId(serviceTemplate.getUuid())
                                              .withComputeProviderSettingId(computeProvider.getUuid())
                                              .withComputeProviderType(computeProvider.getValue().getType())
                                              .withDeploymentType(SSH.name())
                                              .withHostConnectionAttrs(AccessType.KEY.name())
                                              .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
                                              .build());

    triggerWorkflow(app.getAppId(), env, service, infrastructureMapping);
  }

  @Test
  public void shouldTriggerTemplateCanaryWorkflow() throws InterruptedException {
    Service service = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc1").appId(app.getUuid()).build());

    Service templateService = wingsPersistence.saveAndGet(
        Service.class, Service.builder().uuid(generateUuid()).name("svc2").appId(app.getUuid()).build());

    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    SettingAttribute computeProvider = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute().withAppId(app.getUuid()).withValue(aPhysicalDataCenterConfig().build()).build());

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.save(PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                                              .withName("Name2")
                                              .withAppId(app.getUuid())
                                              .withAccountId(app.getAccountId())
                                              .withEnvId(env.getUuid())
                                              .withHostNames(Lists.newArrayList("host1"))
                                              .withServiceTemplateId(serviceTemplate.getUuid())
                                              .withComputeProviderSettingId(computeProvider.getUuid())
                                              .withComputeProviderType(computeProvider.getValue().getType())
                                              .withDeploymentType(SSH.name())
                                              .withHostConnectionAttrs(AccessType.KEY.name())
                                              .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
                                              .build());

    InfrastructureMapping templateInfraMapping =
        infrastructureMappingService.save(PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                                              .withName("Name3")
                                              .withAppId(app.getUuid())
                                              .withEnvId(env.getUuid())
                                              .withAccountId(app.getAccountId())
                                              .withHostNames(Lists.newArrayList("host12"))
                                              .withServiceTemplateId(serviceTemplate.getUuid())
                                              .withComputeProviderSettingId(computeProvider.getUuid())
                                              .withComputeProviderType(computeProvider.getValue().getType())
                                              .withDeploymentType(SSH.name())
                                              .withHostConnectionAttrs(AccessType.KEY.name())
                                              .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
                                              .build());

    triggerTemplateWorkflow(app.getAppId(), env, service, infrastructureMapping, templateService, templateInfraMapping);
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
    execution = workflowExecutionService.getExecutionDetails(appId, executionId, true, emptySet());
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);

    List<StateExecutionInstance> response =
        wingsPersistence
            .query(StateExecutionInstance.class,
                PageRequestBuilder.aPageRequest()
                    .addFilter(StateExecutionInstance.APP_ID_KEY, EQ, appId)
                    .addFilter(StateExecutionInstance.EXECUTION_UUID_KEY, EQ, execution.getUuid())
                    .addFilter(StateExecutionInstance.STATE_TYPE_KEY, EQ, "EMAIL")
                    .build())
            .getResponse();
    assertThat(response).isNotNull().isNotEmpty();
    List<ContextElement> elements = response.get(0)
                                        .getContextElements()
                                        .stream()
                                        .filter(contextElement
                                            -> contextElement.getElementType() == ContextElementType.PARAM
                                                && contextElement.getName() == Constants.PHASE_PARAM)
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
    execution = workflowExecutionService.getExecutionDetails(appId, executionId, true, emptySet());
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
            .withName(WORKFLOW_NAME)
            .withAppId(appId)
            .withEnvId(env.getUuid())
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withName("Phase1")
                                          .withServiceId(service.getUuid())
                                          .withDeploymentType(SSH)
                                          .withInfraMappingId(infrastructureMapping.getUuid())
                                          .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(PhaseStepType.POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
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
        aGraphNode().withType("EMAIL").withName("email").addProperty("toAddress", "a@b.com").build());

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
    TemplateExpression infraExpression = TemplateExpression.builder()
                                             .fieldName("infraMappingId")
                                             .expression("${ServiceInfra_SSH}")
                                             .metadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();

    TemplateExpression serviceExpression = TemplateExpression.builder()
                                               .fieldName("serviceId")
                                               .expression("${Service}")
                                               .metadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();

    Workflow orchestrationWorkflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(appId)
            .withEnvId(env.getUuid())
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withName("Phase1")
                                          .withServiceId(service.getUuid())
                                          .withDeploymentType(SSH)
                                          .withInfraMappingId(infrastructureMapping.getUuid())
                                          .withTemplateExpressions(asList(infraExpression, serviceExpression))
                                          .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(PhaseStepType.POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
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
    assertThat(orchestrationWorkflow1).extracting("userVariables").isNotEmpty();
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
        aGraphNode().withType("EMAIL").withName("email").addProperty("toAddress", "a@b.com").build());

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
  public void shouldObtainNoLastGoodDeployedArtifacts() {
    String appId = app.getUuid();
    Workflow workflow = createExecutableWorkflow(appId, env);
    List<Artifact> artifacts =
        workflowExecutionService.obtainLastGoodDeployedArtifacts(workflow.getAppId(), workflow.getUuid());
    assertThat(artifacts).isEmpty();
  }

  @Test
  public void shouldObtainLastGoodDeployedArtifacts() throws InterruptedException {
    String appId = app.getUuid();
    Workflow workflow = createExecutableWorkflow(appId, env);
    WorkflowExecution workflowExecution = triggerWorkflow(workflow, env);
    assertThat(workflowExecution).isNotNull().hasFieldOrPropertyWithValue("releaseNo", "1");
    List<Artifact> artifacts =
        workflowExecutionService.obtainLastGoodDeployedArtifacts(workflow.getAppId(), workflow.getUuid());
    assertThat(artifacts).isNotEmpty();
  }

  @Test
  @Owner(emails = {"srinivas@harness.io"})
  public void shouldListWaitingOnDeployments() {
    String appId = app.getUuid();
    Workflow workflow = createExecutableWorkflow(appId, env);
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(asList(Artifact.Builder.anArtifact().withAppId(APP_ID).withUuid(ARTIFACT_ID).build()));

    WorkflowExecutionUpdateFake callback = new WorkflowExecutionUpdateFake();
    WorkflowExecution firstExecution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);

    assertThat(firstExecution).isNotNull();

    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback, null);
    assertThat(execution).isNotNull();

    List<WorkflowExecution> waitingOnDeployments =
        workflowExecutionService.listWaitingOnDeployments(appId, execution.getUuid());
    assertThat(waitingOnDeployments).isNotEmpty().hasSize(2);
  }

  @Test
  @Owner(emails = {"srinivas@harness.io"})
  public void shouldFetchWorkflowExecutionStartTs() throws Exception {
    String appId = app.getUuid();
    Workflow workflow = createExecutableWorkflow(appId, env);
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
}
