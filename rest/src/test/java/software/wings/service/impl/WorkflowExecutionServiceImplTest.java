/**
 *
 */

package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.TemplateExpression.Builder.aTemplateExpression;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.harness.rule.RepeatRule.Repeat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.ErrorCode;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Node;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
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
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.Listeners;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyEventListener;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

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
                   .withUuid(getUuid())
                   .withDisplayName(ARTIFACT_NAME)
                   .withServiceIds(new ArrayList<>())
                   .build();
    artifactPageResponse = aPageResponse().withResponse(asList(artifact)).build();
    when(artifactService.list(any(PageRequest.class), eq(false))).thenReturn(artifactPageResponse);
  }

  @Test
  public void shouldTriggerSimpleWorkflow() throws InterruptedException {
    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("n1")
                          .withOrigin(true)
                          .withName("RepeatByInstances")
                          .withX(200)
                          .withY(50)
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances()}")
                          .addProperty("executionStrategyExpression", "${SIMPLE_WORKFLOW_REPEAT_STRATEGY}")
                          .build())
            .addNodes(
                aNode()
                    .withId("n2")
                    .withName("email")
                    .withX(250)
                    .withY(50)
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
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());
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

    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecutionServiceImpl workflowExecutionServiceImpl = (WorkflowExecutionServiceImpl) workflowExecutionService;
    WorkflowExecution workflowExecution =
        workflowExecutionServiceImpl.triggerEnvExecution(app.getUuid(), env.getUuid(), executionArgs, callback);
    callback.await();

    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getUuid()).isNotNull();

    WorkflowExecution workflowExecution2 =
        workflowExecutionService.getExecutionDetails(app.getUuid(), workflowExecution.getUuid());
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
                      .addNodes(aNode()
                                    .withId("n1")
                                    .withOrigin(true)
                                    .withName("RepeatByInstances")
                                    .withX(200)
                                    .withY(50)
                                    .withType(StateType.REPEAT.name())
                                    .addProperty("repeatElementExpression", "${instances()}")
                                    .addProperty("executionStrategyExpression", "${SIMPLE_WORKFLOW_REPEAT_STRATEGY}")
                                    .build())
                      .addNodes(aNode()
                                    .withId("n2")
                                    .withName("stop")
                                    .withX(250)
                                    .withY(50)
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
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());
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

    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecutionServiceImpl workflowExecutionServiceImpl = (WorkflowExecutionServiceImpl) workflowExecutionService;
    WorkflowExecution workflowExecution =
        workflowExecutionServiceImpl.triggerEnvExecution(app.getUuid(), env.getUuid(), executionArgs, callback);
    callback.await();

    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getUuid()).isNotNull();

    WorkflowExecution workflowExecution2 =
        workflowExecutionService.getExecutionDetails(app.getUuid(), workflowExecution.getUuid());
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

    PageRequest<WorkflowExecution> pageRequest = PageRequest.Builder.aPageRequest()
                                                     .addFilter("appId", Operator.EQ, app.getUuid())
                                                     .addFilter("uuid", Operator.EQ, workflowExecution.getUuid())
                                                     .build();
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
  public void shouldTriggerComplexWorkflow() throws InterruptedException {
    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host host2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service1 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    Service service2 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(getUuid()).withName("svc2").withAppId(app.getUuid()).build());
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
            .addNodes(aNode()
                          .withId("Repeat By Services")
                          .withOrigin(true)
                          .withName("Repeat By Services")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build())
            .addNodes(aNode()
                          .withId("RepeatByInstances")
                          .withName("RepeatByInstances")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("svcRepeatWait")
                          .withName("svcRepeatWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("instRepeatWait")
                          .withName("instRepeatWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
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
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback);
    callback.await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    assertThat(execution.getExecutionNode())
        .isNotNull()
        .extracting("name", "type", "status")
        .containsExactly("Repeat By Services", "REPEAT", "SUCCESS");
    assertThat(execution.getExecutionNode().getGroup()).isNotNull();
    assertThat(execution.getExecutionNode().getGroup().getElements()).isNotNull().doesNotContainNull().hasSize(2);

    List<Node> svcElements = execution.getExecutionNode().getGroup().getElements();
    assertThat(svcElements).isNotNull().hasSize(2).extracting("name").contains(service1.getName(), service2.getName());
    assertThat(svcElements).extracting("type").contains("ELEMENT", "ELEMENT");

    List<Node> svcRepeatWaits = svcElements.stream().map(Node::getNext).collect(Collectors.toList());
    assertThat(svcRepeatWaits).isNotNull().hasSize(2).extracting("name").contains("svcRepeatWait", "svcRepeatWait");
    assertThat(svcRepeatWaits).extracting("type").contains("WAIT", "WAIT");

    List<Node> repeatInstance = svcRepeatWaits.stream().map(Node::getNext).collect(Collectors.toList());
    assertThat(repeatInstance)
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("RepeatByInstances", "RepeatByInstances");
    assertThat(repeatInstance).extracting("type").contains("REPEAT", "REPEAT");

    List<Node> instSuccessWait = repeatInstance.stream().map(Node::getNext).collect(Collectors.toList());
    assertThat(instSuccessWait)
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("instSuccessWait", "instSuccessWait");
    assertThat(instSuccessWait).extracting("type").contains("WAIT", "WAIT");

    List<Node> instRepeatElements = repeatInstance.stream()
                                        .map(Node::getGroup)
                                        .flatMap(group -> group.getElements().stream())
                                        .collect(Collectors.toList());
    assertThat(instRepeatElements).extracting("type").contains("ELEMENT", "ELEMENT", "ELEMENT", "ELEMENT");

    List<Node> instRepeatWait = instRepeatElements.stream().map(Node::getNext).collect(Collectors.toList());
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
  public void triggerPipeline() throws InterruptedException {
    Host host = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());
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
            .addNodes(aNode()
                          .withId("Repeat By Services")
                          .withOrigin(true)
                          .withName("Repeat By Services")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build())
            .addNodes(aNode()
                          .withId("RepeatByInstances")
                          .withName("RepeatByInstances")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("svcRepeatWait")
                          .withName("svcRepeatWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("instRepeatWait")
                          .withName("instRepeatWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
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
            .withEnvId(ENV_ID)
            .withAppId(app.getUuid())
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(aCustomOrchestrationWorkflow().withValid(true).withGraph(graph).build())
            .build();
    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();

    PipelineStage stag1 = new PipelineStage(asList(new PipelineStageElement(
        "DEV", StateType.ENV_STATE.name(), ImmutableMap.of("envId", env.getUuid(), "workflowId", workflow.getUuid()))));
    // PipelineStage stag2 = new PipelineStage(asList(new PipelineStageElement("APPROVAL", StateType.APPROVAL.name(),
    // ImmutableMap.of("envId", env.getUuid()))));
    List<PipelineStage> pipelineStages = asList(stag1);

    Pipeline pipeline = aPipeline()
                            .withAppId(app.getUuid())
                            .withName("pipeline1")
                            .withDescription("Sample Pipeline")
                            .withPipelineStages(pipelineStages)
                            .build();

    pipeline = pipelineService.createPipeline(pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.listStateMachines(req);

    assertThat(res).isNotNull().hasSize(1).doesNotContainNull();
    assertThat(res.get(0).getTransitions()).hasSize(0);

    Artifact artifact = wingsPersistence.saveAndGet(Artifact.class,
        anArtifact()
            .withAppId(app.getUuid())
            .withDisplayName(ARTIFACT_NAME)
            .withServiceIds(asList(service.getUuid()))
            .build());
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(asList(artifact));

    triggerPipeline(app.getUuid(), pipeline, executionArgs);
  }

  private WorkflowExecution triggerPipeline(String appId, Pipeline pipeline, ExecutionArgs executionArgs)
      throws InterruptedException {
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerPipelineExecution(appId, pipeline.getUuid(), executionArgs, callback);
    callback.await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Pipeline executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId);
    assertThat(execution)
        .isNotNull()
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
   * Should get node details.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldGetNodeDetails() throws InterruptedException {
    String appId = app.getUuid();

    WorkflowExecution execution = workflowExecutionService.getExecutionDetails(appId, triggerWorkflow(appId, env));
    Node node0 = execution.getExecutionNode();
    assertThat(workflowExecutionService.getExecutionDetailsForNode(appId, execution.getUuid(), node0.getId()))
        .isEqualToIgnoringGivenFields(node0, "x", "y", "width", "height", "next", "expanded");
  }

  /**
   * Should update in progress count.
   *
   * @throws InterruptedException the interrupted exception
   */
  //  @Test
  //  public void shouldUpdateInProgressCount() throws InterruptedException {
  //    Environment env = wingsPersistence.saveAndGet(Environment.class,
  //    Builder.anEnvironment().withAppId(appId).build()); triggerWorkflow(env); WorkflowExecution workflowExecution =
  //    wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
  //    workflowExecutionService.incrementInProgressCount(workflowExecution.getAccountId(), workflowExecution.getUuid(),
  //    1); workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
  //    assertThat(workflowExecution.getBreakdown().getInprogress()).isEqualTo(1);
  //  }

  /**
   * Should update success count.
   *
   * @throws InterruptedException the interrupted exception
   */
  //  @Test
  //  public void shouldUpdateSuccessCount() throws InterruptedException {
  //    Environment env = wingsPersistence.saveAndGet(Environment.class,
  //    Builder.anEnvironment().withAppId(appId).build()); triggerWorkflow(env); WorkflowExecution workflowExecution =
  //    wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
  //    workflowExecutionService.incrementSuccess(workflowExecution.getAccountId(), workflowExecution.getUuid(), 1);
  //    workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
  //    assertThat(workflowExecution.getBreakdown().getSuccess()).isEqualTo(2);
  //  }

  /**
   * Should update failed count.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldUpdateFailedCount() throws InterruptedException {
    String appId = app.getUuid();
    triggerWorkflow(appId, env);
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
    workflowExecutionService.incrementFailed(workflowExecution.getAppId(), workflowExecution.getUuid(), 1);
    workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
    assertThat(workflowExecution.getBreakdown().getFailed()).isEqualTo(1);
    System.out.println("shouldUpdateFailedCount test done");
  }

  /**
   * Trigger workflow.
   *
   * @param appId the app id
   * @param env   the env
   * @return the string
   * @throws InterruptedException the interrupted exception
   */
  public String triggerWorkflow(String appId, Environment env) throws InterruptedException {
    Workflow workflow = createExecutableWorkflow(appId, env);
    ExecutionArgs executionArgs = new ExecutionArgs();

    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback);
    callback.await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    return executionId;
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

    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback);
    callback.await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    return executionId;
  }

  private Workflow createExecutableWorkflow(String appId, Environment env) {
    Graph graph = aGraph()
                      .addNodes(aNode()
                                    .withId("n1")
                                    .withName("wait")
                                    .withX(200)
                                    .withY(50)
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1l)
                                    .withOrigin(true)
                                    .build())
                      .addNodes(aNode()
                                    .withId("n2")
                                    .withName("email")
                                    .withX(250)
                                    .withY(50)
                                    .withType(StateType.EMAIL.name())
                                    .addProperty("toAddress", "a@b.com")
                                    .addProperty("subject", "testing")
                                    .build())
                      .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
                      .build();

    Workflow workflow =
        aWorkflow()
            .withEnvId(ENV_ID)
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
    Workflow workflow = createExecutableWorkflow(appId, env);
    PageRequest<Workflow> pageRequest = new PageRequest<>();
    PageResponse<Workflow> res = workflowService.listWorkflows(pageRequest, null);

    assertThat(res).isNotNull().hasSize(2);
  }

  /**
   * Should pause and resume
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Repeat(times = 2, successes = 1)
  public void shouldPauseAndResumeState() throws InterruptedException {
    Graph graph = aGraph()
                      .addNodes(aNode()
                                    .withId("wait1")
                                    .withOrigin(true)
                                    .withName("wait1")
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1)
                                    .build())
                      .addNodes(aNode()
                                    .withId("pause1")
                                    .withName("pause1")
                                    .withType(StateType.PAUSE.name())
                                    .addProperty("toAddress", "to1")
                                    .build())
                      .addNodes(aNode()
                                    .withId("wait2")
                                    .withName("wait2")
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1)
                                    .build())
                      .addLinks(aLink().withId("l1").withFrom("wait1").withTo("pause1").withType("success").build())
                      .addLinks(aLink().withId("l2").withFrom("pause1").withTo("wait2").withType("success").build())
                      .build();

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
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    int i = 0;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    } while (execution.getStatus() != ExecutionStatus.PAUSED && i < 5);
    Thread.sleep(1000);
    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);

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
        ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withExecutionUuid(executionId)
            .withStateExecutionInstanceId(execution.getExecutionNode().getNext().getId())
            .withExecutionInterruptType(ExecutionInterruptType.RESUME)
            .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
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
  // TODO - Fix this, it's failing in Jenkins
  @Test
  @Ignore
  public void shouldPauseAllAndResumeAllState() throws InterruptedException {
    Service service1 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    Service service2 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(getUuid()).withName("svc2").withAppId(app.getUuid()).build());

    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("wait1")
                          .withName("wait1")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("wait2")
                          .withName("wait2")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addLinks(aLink().withId("l1").withFrom("RepeatByServices").withTo("wait1").withType("repeat").build())
            .addLinks(aLink().withId("l2").withFrom("wait1").withTo("wait2").withType("success").build())
            .build();

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
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    Thread.sleep(2000);

    ExecutionInterrupt executionInterrupt = ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                                .withExecutionUuid(executionId)
                                                .withEnvId(env.getUuid())
                                                .build();
    executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    assertThat(executionInterrupt).isNotNull().hasFieldOrProperty("uuid");

    int i = 0;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    } while (
        (execution.getStatus() != ExecutionStatus.PAUSED && i < 15) || execution.getExecutionNode().getGroup() == null);

    Thread.sleep(2000);
    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);

    List<Node> wait1List = execution.getExecutionNode()
                               .getGroup()
                               .getElements()
                               .stream()
                               .filter(n -> n.getNext() != null)
                               .map(Node::getNext)
                               .collect(Collectors.toList());
    List<Node> wait2List =
        wait1List.stream().filter(n -> n.getNext() != null).map(Node::getNext).collect(Collectors.toList());

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

    executionInterrupt = ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
                             .withAppId(app.getUuid())
                             .withExecutionInterruptType(ExecutionInterruptType.RESUME_ALL)
                             .withExecutionUuid(executionId)
                             .withEnvId(env.getUuid())
                             .build();
    executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    assertThat(executionInterrupt).isNotNull().hasFieldOrProperty("uuid");

    callback.await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.SUCCESS);

    wait1List = execution.getExecutionNode()
                    .getGroup()
                    .getElements()
                    .stream()
                    .filter(n -> n.getNext() != null)
                    .map(Node::getNext)
                    .collect(Collectors.toList());
    wait2List = wait1List.stream().filter(n -> n.getNext() != null).map(Node::getNext).collect(Collectors.toList());

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

  /**
   * Should throw invalid argument for invalid workflow id.
   */
  @Test
  public void shouldThrowInvalidArgumentForInvalidWorkflowId() {
    ExecutionInterrupt executionInterrupt = ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.PAUSE)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(getUuid())
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
      assertThat(exception).hasMessage(ErrorCode.INVALID_ARGUMENT.getCode());
    }
  }

  /**
   * Should abort
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Ignore // TODO: fix this. It seems there is production issues
  public void shouldAbortState() throws InterruptedException {
    Graph graph = aGraph()
                      .addNodes(aNode()
                                    .withId("wait1")
                                    .withOrigin(true)
                                    .withName("wait1")
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1)
                                    .build())
                      .addNodes(aNode()
                                    .withId("pause1")
                                    .withName("pause1")
                                    .withType(StateType.PAUSE.name())
                                    .addProperty("toAddress", "to1")
                                    .build())
                      .addNodes(aNode()
                                    .withId("wait2")
                                    .withName("wait2")
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1)
                                    .build())
                      .addLinks(aLink().withId("l1").withFrom("wait1").withTo("pause1").withType("success").build())
                      .addLinks(aLink().withId("l2").withFrom("pause1").withTo("wait2").withType("success").build())
                      .build();

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
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    int i = 0;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    } while (execution.getStatus() != ExecutionStatus.PAUSED && i < 5);
    Thread.sleep(1000);
    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
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
        ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withExecutionUuid(executionId)
            .withStateExecutionInstanceId(execution.getExecutionNode().getNext().getId())
            .withExecutionInterruptType(ExecutionInterruptType.ABORT)
            .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.ABORTED);

    assertThat(execution.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "wait1")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(execution.getExecutionNode().getNext())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "pause1")
        .hasFieldOrPropertyWithValue("status", "ABORTED");
  }

  /**
   * Should abort all
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Ignore
  public void shouldAbortAllStates() throws InterruptedException {
    Service service1 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    Service service2 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(getUuid()).withName("svc2").withAppId(app.getUuid()).build());

    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("wait1")
                          .withName("wait1")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("wait2")
                          .withName("wait2")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addLinks(aLink().withId("l1").withFrom("RepeatByServices").withTo("wait1").withType("repeat").build())
            .addLinks(aLink().withId("l2").withFrom("wait1").withTo("wait2").withType("success").build())
            .build();

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
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    Thread.sleep(3000);

    ExecutionInterrupt executionInterrupt = ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.ABORT_ALL)
                                                .withExecutionUuid(executionId)
                                                .withEnvId(env.getUuid())
                                                .build();
    executionInterrupt = workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    assertThat(executionInterrupt).isNotNull().hasFieldOrProperty("uuid");

    int i = 0;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    } while ((execution.getStatus() != ExecutionStatus.ABORTED && i < 15)
        || execution.getExecutionNode().getGroup() == null);
    Thread.sleep(1000);
    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);

    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.ABORTED);

    assertThat(execution.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "RepeatByServices")
        .hasFieldOrPropertyWithValue("status", "ABORTED");
    assertThat(execution.getExecutionNode().getGroup()).isNotNull();
    assertThat(execution.getExecutionNode().getGroup().getElements())
        .isNotNull()
        .hasSize(2)
        .extracting("next")
        .doesNotContainNull()
        .extracting("name")
        .contains("wait1", "wait1");
    assertThat(execution.getExecutionNode().getGroup().getElements())
        .isNotNull()
        .hasSize(2)
        .extracting("next")
        .doesNotContainNull()
        .extracting("status")
        .contains("ABORTED", "ABORTED");
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
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());
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
            .addNodes(aNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("RepeatByInstances")
                          .withName("RepeatByInstances")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build())
            .addNodes(aNode()
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
    executionArgs.setErrorStrategy(ErrorStrategy.PAUSE);
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationWorkflowExecution(app.getUuid(), env.getUuid(),
                                          workflow.getUuid(), null, executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    List<Node> installNodes = getNodes(executionId);

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.WAITING);
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(ExecutionStatus.WAITING.name());

    Node installNode = installNodes.get(0);
    ExecutionInterrupt executionInterrupt = ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
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
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.WAITING);
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(2)
        .extracting("status")
        .contains(ExecutionStatus.SUCCESS.name(), ExecutionStatus.WAITING.name());

    installNode = installNodes.stream()
                      .filter(n -> n.getStatus() != null && n.getStatus().equals(ExecutionStatus.WAITING.name()))
                      .collect(Collectors.toList())
                      .get(0);
    executionInterrupt = ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
                             .withAppId(app.getUuid())
                             .withEnvId(env.getUuid())
                             .withExecutionUuid(executionId)
                             .withStateExecutionInstanceId(installNode.getId())
                             .withExecutionInterruptType(ExecutionInterruptType.IGNORE)
                             .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.SUCCESS);
    installNodes = execution.getExecutionNode()
                       .getGroup()
                       .getElements()
                       .stream()
                       .filter(node -> node.getNext() != null)
                       .map(Node::getNext)
                       .filter(node -> node.getGroup() != null)
                       .map(Node::getGroup)
                       .filter(group -> group.getElements() != null)
                       .flatMap(group -> group.getElements().stream())
                       .filter(node -> node.getNext() != null)
                       .map(Node::getNext)
                       .collect(Collectors.toList());
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(2)
        .extracting("status")
        .containsExactly(ExecutionStatus.SUCCESS.name(), ExecutionStatus.FAILED.name());
  }

  private List<Node> getNodes(String executionId) throws InterruptedException {
    WorkflowExecution execution;
    int i = 0;
    List<Node> installNodes = null;
    boolean paused = false;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);

      if (execution.getExecutionNode() == null || execution.getExecutionNode().getGroup() == null
          || execution.getExecutionNode().getGroup().getElements() == null) {
        continue;
      }
      installNodes = execution.getExecutionNode()
                         .getGroup()
                         .getElements()
                         .stream()
                         .filter(node -> node.getNext() != null)
                         .map(Node::getNext)
                         .filter(node -> node.getGroup() != null)
                         .map(Node::getGroup)
                         .filter(group -> group.getElements() != null)
                         .flatMap(group -> group.getElements().stream())
                         .filter(node -> node.getNext() != null)
                         .map(Node::getNext)
                         .collect(Collectors.toList());
      paused = !installNodes.stream()
                    .filter(n -> n.getStatus() != null && n.getStatus().equals(ExecutionStatus.WAITING.name()))
                    .collect(Collectors.toList())
                    .isEmpty();
    } while (!paused && i < 5);
    return installNodes;
  }

  /**
   * Should retry on error
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldRetryOnError() throws InterruptedException {
    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host1").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());
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
            .addNodes(aNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("RepeatByInstances")
                          .withName("RepeatByInstances")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build())
            .addNodes(aNode()
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
    executionArgs.setErrorStrategy(ErrorStrategy.PAUSE);
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        app.getUuid(), env.getUuid(), workflow.getUuid(), null, executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    List<Node> installNodes = getNodes(executionId);

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.WAITING);

    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(ExecutionStatus.WAITING.name());

    Node installNode = installNodes.get(0);
    ExecutionInterrupt executionInterrupt = ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
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
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.WAITING);
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(ExecutionStatus.WAITING.name());

    installNode = installNodes.get(0);
    executionInterrupt = ExecutionInterrupt.Builder.aWorkflowExecutionInterrupt()
                             .withAppId(app.getUuid())
                             .withEnvId(env.getUuid())
                             .withExecutionUuid(executionId)
                             .withStateExecutionInstanceId(installNode.getId())
                             .withExecutionInterruptType(ExecutionInterruptType.MARK_SUCCESS)
                             .build();
    workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
    callback.await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.SUCCESS);

    installNodes = execution.getExecutionNode()
                       .getGroup()
                       .getElements()
                       .stream()
                       .filter(node -> node.getNext() != null)
                       .map(Node::getNext)
                       .filter(node -> node.getGroup() != null)
                       .map(Node::getGroup)
                       .filter(group -> group.getElements() != null)
                       .flatMap(group -> group.getElements().stream())
                       .filter(node -> node.getNext() != null)
                       .map(Node::getNext)
                       .collect(Collectors.toList());
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
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());

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
        Service.class, aService().withUuid(getUuid()).withName("svc1").withAppId(app.getUuid()).build());

    Service templateService = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(getUuid()).withName("svc2").withAppId(app.getUuid()).build());

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

    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback);
    callback.await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
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

    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock();
    WorkflowExecution execution = workflowExecutionService.triggerOrchestrationWorkflowExecution(
        appId, env.getUuid(), workflow.getUuid(), null, executionArgs, callback);
    callback.await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Workflow executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId);
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
                                    .collect(Collectors.toList())
                                    .get(0);

    deployPhaseStep.getSteps().add(
        aNode().withType("EMAIL").withName("email").addProperty("toAddress", "a@b.com").build());

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
    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();

    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
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
                                    .collect(Collectors.toList())
                                    .get(0);

    deployPhaseStep.getSteps().add(
        aNode().withType("EMAIL").withName("email").addProperty("toAddress", "a@b.com").build());

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
}
