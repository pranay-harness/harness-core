package software.wings.service;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.CountsByStatuses.Builder.aCountsByStatuses;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateMachine.StateMachineBuilder.aStateMachine;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DEFAULT_VERSION;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsTestConstants.STATE_MACHINE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.mongodb.WriteResult;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.WingsBaseTest;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.ServiceInstance;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.command.Command;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.Listeners;
import software.wings.service.impl.GraphRenderer;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.waitnotify.NotifyEventListener;

import java.util.List;
import java.util.Map;

/**
 * The Class workflowExecutionServiceTest.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowExecutionServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private WorkflowExecutionService workflowExecutionService;

  @Mock private WingsPersistence wingsPersistence;
  @Mock private WorkflowService workflowService;

  @Mock private ServiceResourceService serviceResourceServiceMock;
  @Mock private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Mock private GraphRenderer graphRenderer;

  private Workflow workflow =
      aWorkflow()
          .withUuid(WORKFLOW_ID)
          .withAppId(APP_ID)
          .withName(WORKFLOW_NAME)
          .withDefaultVersion(DEFAULT_VERSION)
          .withOrchestrationWorkflow(
              aCanaryOrchestrationWorkflow()
                  .withRequiredEntityTypes(Sets.newHashSet(EntityType.SSH_USER, EntityType.SSH_PASSWORD))
                  .build())
          .build();

  @Mock Query<WorkflowExecution> query;
  @Mock private FieldEnd end;
  @Mock private UpdateOperations<WorkflowExecution> updateOperations;
  @Mock private UpdateResults updateResults;
  @Mock WriteResult writeResult;

  /**
   * test setup.
   */
  @Before
  public void setUp() {
    when(wingsPersistence.createQuery(eq(WorkflowExecution.class))).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);

    when(wingsPersistence.createUpdateOperations(WorkflowExecution.class)).thenReturn(updateOperations);
    when(updateOperations.set(anyString(), any())).thenReturn(updateOperations);
    when(updateOperations.addToSet(anyString(), any())).thenReturn(updateOperations);
    when(wingsPersistence.update(query, updateOperations)).thenReturn(updateResults);
    when(updateResults.getWriteResult()).thenReturn(writeResult);
    when(writeResult.getN()).thenReturn(1);
  }

  @Test
  public void shouldListExecutions() {
    PageRequest<WorkflowExecution> pageRequest = aPageRequest().build();
    PageResponse<WorkflowExecution> pageResponse = aPageResponse().build();
    when(wingsPersistence.query(WorkflowExecution.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<WorkflowExecution> pageResponse2 =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, true);
    assertThat(pageResponse2).isNotNull().isEqualTo(pageResponse);
    verify(wingsPersistence).query(WorkflowExecution.class, pageRequest);
  }

  @Test
  public void shouldListCompletedExecutions() {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    when(wingsPersistence.get(StateMachine.class, APP_ID, STATE_MACHINE_ID)).thenReturn(aStateMachine().build());
    PageRequest<WorkflowExecution> pageRequest = aPageRequest().build();
    PageResponse<WorkflowExecution> pageResponse = aPageResponse()
                                                       .withResponse(asList(aWorkflowExecution()
                                                                                .withAppId(APP_ID)
                                                                                .withUuid(getUuid())
                                                                                .withEnvId(ENV_ID)
                                                                                .withStateMachineId(STATE_MACHINE_ID)
                                                                                .withStatus(SUCCESS)
                                                                                .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(getUuid())
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(SUCCESS)
                                                               .withBreakdown(countsByStatuses)
                                                               .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(getUuid())
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(ExecutionStatus.FAILED)
                                                               .withBreakdown(countsByStatuses)
                                                               .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(getUuid())
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(ExecutionStatus.ABORTED)
                                                               .build()))
                                                       .build();
    when(wingsPersistence.query(WorkflowExecution.class, pageRequest)).thenReturn(pageResponse);

    PageResponse<StateExecutionInstance> stateExecutionInstancePageResponse = aPageResponse().build();
    when(wingsPersistence.queryAll(eq(StateExecutionInstance.class), any(PageRequest.class)))
        .thenReturn(stateExecutionInstancePageResponse.getResponse());

    when(stateMachineExecutionSimulator.getStatusBreakdown(
             eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class)))
        .thenReturn(countsByStatuses);

    PageResponse<WorkflowExecution> pageResponse2 =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, true);
    assertThat(pageResponse2).isNotNull().isEqualTo(pageResponse);
    verify(wingsPersistence, times(1)).query(WorkflowExecution.class, pageRequest);
    verify(wingsPersistence, times(2)).queryAll(eq(StateExecutionInstance.class), any(PageRequest.class));
    verify(updateOperations, times(2)).set("breakdown", countsByStatuses);
    verify(stateMachineExecutionSimulator, times(2))
        .getStatusBreakdown(eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class));
  }

  @Test
  public void shouldListRunningExecutions() {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    when(wingsPersistence.get(StateMachine.class, APP_ID, STATE_MACHINE_ID)).thenReturn(aStateMachine().build());
    PageRequest<WorkflowExecution> pageRequest = aPageRequest().build();
    PageResponse<WorkflowExecution> pageResponse = aPageResponse()
                                                       .withResponse(asList(aWorkflowExecution()
                                                                                .withAppId(APP_ID)
                                                                                .withUuid(getUuid())
                                                                                .withEnvId(ENV_ID)
                                                                                .withStateMachineId(STATE_MACHINE_ID)
                                                                                .withStatus(SUCCESS)
                                                                                .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(getUuid())
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(SUCCESS)
                                                               .withBreakdown(countsByStatuses)
                                                               .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(getUuid())
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(ExecutionStatus.FAILED)
                                                               .withBreakdown(countsByStatuses)
                                                               .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(getUuid())
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(ExecutionStatus.RUNNING)
                                                               .build()))
                                                       .build();
    when(wingsPersistence.query(WorkflowExecution.class, pageRequest)).thenReturn(pageResponse);

    PageResponse<StateExecutionInstance> stateExecutionInstancePageResponse = aPageResponse().build();
    when(wingsPersistence.queryAll(eq(StateExecutionInstance.class), any(PageRequest.class)))
        .thenReturn(stateExecutionInstancePageResponse.getResponse());

    when(stateMachineExecutionSimulator.getStatusBreakdown(
             eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class)))
        .thenReturn(countsByStatuses);

    PageResponse<WorkflowExecution> pageResponse2 =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, true);
    assertThat(pageResponse2).isNotNull().isEqualTo(pageResponse);
    verify(wingsPersistence, times(1)).query(WorkflowExecution.class, pageRequest);
    verify(wingsPersistence, times(3)).queryAll(eq(StateExecutionInstance.class), any(PageRequest.class));
    verify(updateOperations, times(1)).set("breakdown", countsByStatuses);
    verify(stateMachineExecutionSimulator, times(2))
        .getStatusBreakdown(eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class));
  }

  @Test
  public void shouldListPausedExecutions() {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    when(wingsPersistence.get(StateMachine.class, APP_ID, STATE_MACHINE_ID)).thenReturn(aStateMachine().build());
    PageRequest<WorkflowExecution> pageRequest = aPageRequest().build();
    PageResponse<WorkflowExecution> pageResponse = aPageResponse()
                                                       .withResponse(asList(aWorkflowExecution()
                                                                                .withAppId(APP_ID)
                                                                                .withUuid(getUuid())
                                                                                .withEnvId(ENV_ID)
                                                                                .withStateMachineId(STATE_MACHINE_ID)
                                                                                .withStatus(SUCCESS)
                                                                                .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(getUuid())
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(SUCCESS)
                                                               .withBreakdown(countsByStatuses)
                                                               .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(WORKFLOW_ID)
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(ExecutionStatus.RUNNING)
                                                               .build()))
                                                       .build();
    when(wingsPersistence.query(WorkflowExecution.class, pageRequest)).thenReturn(pageResponse);

    PageResponse<StateExecutionInstance> stateExecutionInstancePageResponse =
        aPageResponse()
            .withResponse(asList(aStateExecutionInstance().withUuid(getUuid()).withStatus(SUCCESS).build(),
                aStateExecutionInstance().withUuid(getUuid()).withStatus(ExecutionStatus.PAUSED).build(),
                aStateExecutionInstance().withUuid(getUuid()).withStatus(ExecutionStatus.FAILED).build()))
            .build();
    when(wingsPersistence.queryAll(eq(StateExecutionInstance.class), any(PageRequest.class)))
        .thenReturn(stateExecutionInstancePageResponse.getResponse());

    when(stateMachineExecutionSimulator.getStatusBreakdown(
             eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class)))
        .thenReturn(countsByStatuses);

    PageResponse<WorkflowExecution> pageResponse2 =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, true);
    assertThat(pageResponse2)
        .isNotNull()
        .isEqualTo(pageResponse)
        .extracting(WorkflowExecution::getStatus)
        .containsExactly(SUCCESS, SUCCESS, ExecutionStatus.PAUSED);
    verify(wingsPersistence, times(1)).query(WorkflowExecution.class, pageRequest);
    verify(wingsPersistence, times(3)).queryAll(eq(StateExecutionInstance.class), any(PageRequest.class));
    verify(updateOperations, times(1)).set("breakdown", countsByStatuses);
    verify(stateMachineExecutionSimulator, times(2))
        .getStatusBreakdown(eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class));
  }

  @Test
  public void shouldListWaitingExecutions() {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    when(wingsPersistence.get(StateMachine.class, APP_ID, STATE_MACHINE_ID)).thenReturn(aStateMachine().build());
    PageRequest<WorkflowExecution> pageRequest = aPageRequest().build();
    PageResponse<WorkflowExecution> pageResponse = aPageResponse()
                                                       .withTotal(3)
                                                       .withResponse(asList(aWorkflowExecution()
                                                                                .withAppId(APP_ID)
                                                                                .withUuid(getUuid())
                                                                                .withEnvId(ENV_ID)
                                                                                .withStateMachineId(STATE_MACHINE_ID)
                                                                                .withStatus(SUCCESS)
                                                                                .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(getUuid())
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(SUCCESS)
                                                               .withBreakdown(countsByStatuses)
                                                               .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(WORKFLOW_ID)
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(ExecutionStatus.RUNNING)
                                                               .build()))
                                                       .build();
    when(wingsPersistence.query(WorkflowExecution.class, pageRequest)).thenReturn(pageResponse);

    PageResponse<StateExecutionInstance> stateExecutionInstancePageResponse =
        aPageResponse()
            .withTotal(3)
            .withResponse(asList(aStateExecutionInstance().withUuid(getUuid()).withStatus(SUCCESS).build(),
                aStateExecutionInstance().withUuid(getUuid()).withStatus(WAITING).build(),
                aStateExecutionInstance().withUuid(getUuid()).withStatus(ExecutionStatus.FAILED).build()))
            .build();
    when(wingsPersistence.queryAll(eq(StateExecutionInstance.class), any(PageRequest.class)))
        .thenReturn(stateExecutionInstancePageResponse.getResponse());

    when(stateMachineExecutionSimulator.getStatusBreakdown(
             eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class)))
        .thenReturn(countsByStatuses);

    PageResponse<WorkflowExecution> pageResponse2 =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, true);
    assertThat(pageResponse2)
        .isNotNull()
        .isEqualTo(pageResponse)
        .extracting(WorkflowExecution::getStatus)
        .containsExactly(SUCCESS, SUCCESS, WAITING);
    verify(wingsPersistence, times(1)).query(WorkflowExecution.class, pageRequest);
    verify(wingsPersistence, times(3)).queryAll(eq(StateExecutionInstance.class), any(PageRequest.class));
    verify(updateOperations, times(1)).set("breakdown", countsByStatuses);
    verify(stateMachineExecutionSimulator, times(2))
        .getStatusBreakdown(eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class));
  }

  @Test
  public void populateGraphForRunningExecutions() {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    when(wingsPersistence.get(StateMachine.class, APP_ID, STATE_MACHINE_ID)).thenReturn(aStateMachine().build());
    PageRequest<WorkflowExecution> pageRequest = aPageRequest().build();
    PageResponse<WorkflowExecution> pageResponse = aPageResponse()
                                                       .withResponse(asList(aWorkflowExecution()
                                                                                .withAppId(APP_ID)
                                                                                .withUuid(getUuid())
                                                                                .withEnvId(ENV_ID)
                                                                                .withStateMachineId(STATE_MACHINE_ID)
                                                                                .withStatus(SUCCESS)
                                                                                .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(getUuid())
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(SUCCESS)
                                                               .withBreakdown(countsByStatuses)
                                                               .build(),
                                                           aWorkflowExecution()
                                                               .withAppId(APP_ID)
                                                               .withUuid(WORKFLOW_ID)
                                                               .withEnvId(ENV_ID)
                                                               .withStateMachineId(STATE_MACHINE_ID)
                                                               .withStatus(ExecutionStatus.RUNNING)
                                                               .build()))
                                                       .build();
    when(wingsPersistence.query(WorkflowExecution.class, pageRequest)).thenReturn(pageResponse);

    List<StateExecutionInstance> stateExecutionInstances =
        asList(aStateExecutionInstance().withUuid(getUuid()).withStatus(SUCCESS).build(),
            aStateExecutionInstance().withUuid(getUuid()).withStatus(ExecutionStatus.RUNNING).build(),
            aStateExecutionInstance().withUuid(getUuid()).withStatus(ExecutionStatus.FAILED).build());
    Map<String, StateExecutionInstance> stateExecutionInstanceMap =
        stateExecutionInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));

    PageResponse<StateExecutionInstance> stateExecutionInstancePageResponse =
        aPageResponse().withResponse(stateExecutionInstances).build();
    when(wingsPersistence.queryAll(eq(StateExecutionInstance.class), any(PageRequest.class)))
        .thenReturn(stateExecutionInstancePageResponse.getResponse());

    when(stateMachineExecutionSimulator.getStatusBreakdown(
             eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class)))
        .thenReturn(countsByStatuses);
    GraphNode node = aGraphNode().build();
    when(graphRenderer.generateHierarchyNode(stateExecutionInstanceMap, null)).thenReturn(node);
    PageResponse<WorkflowExecution> pageResponse2 =
        workflowExecutionService.listExecutions(pageRequest, true, true, false, true);
    assertThat(pageResponse2)
        .isNotNull()
        .isEqualTo(pageResponse)
        .extracting(WorkflowExecution::getStatus)
        .containsExactly(SUCCESS, SUCCESS, RUNNING);
    assertThat(pageResponse2.get(2).getExecutionNode()).isEqualTo(node);
    verify(wingsPersistence, times(1)).query(WorkflowExecution.class, pageRequest);
    verify(wingsPersistence, times(3)).queryAll(eq(StateExecutionInstance.class), any(PageRequest.class));
    verify(updateOperations, times(1)).set("breakdown", countsByStatuses);
    verify(stateMachineExecutionSimulator, times(2))
        .getStatusBreakdown(eq(APP_ID), eq(ENV_ID), any(StateMachine.class), any(PageResponse.class));
    verify(graphRenderer, times(1)).generateHierarchyNode(stateExecutionInstanceMap, null);
  }

  /**
   * Required execution args for simple workflow start.
   */
  @Test
  public void requiredExecutionArgsForSimpleWorkflowStart() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.SIMPLE);

    executionArgs.setServiceId(SERVICE_ID);

    String commandName = "Start";
    executionArgs.setCommandName(commandName);

    ServiceInstance inst1 = ServiceInstance.Builder.aServiceInstance().withUuid(getUuid()).build();
    ServiceInstance inst2 = ServiceInstance.Builder.aServiceInstance().withUuid(getUuid()).build();
    executionArgs.setServiceInstances(Lists.newArrayList(inst1, inst2));

    when(stateMachineExecutionSimulator.getInfrastructureRequiredEntityType(
             APP_ID, Lists.newArrayList(inst1.getUuid(), inst2.getUuid())))
        .thenReturn(Sets.newHashSet(EntityType.SSH_USER, EntityType.SSH_PASSWORD));

    Command cmd = mock(Command.class);
    when(cmd.isArtifactNeeded()).thenReturn(false);
    when(serviceResourceServiceMock.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "Start"))
        .thenReturn(aServiceCommand().withTargetToAllEnv(true).withCommand(cmd).build());

    RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
    assertThat(required).isNotNull();
    assertThat(required.getEntityTypes()).isNotNull().hasSize(2).contains(EntityType.SSH_USER, EntityType.SSH_PASSWORD);
  }

  /**
   * Required execution args for orchestrated workflow.
   */
  @Test
  public void requiredExecutionArgsForOrchestratedWorkflow() {
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.readStateMachine(APP_ID, WORKFLOW_ID, DEFAULT_VERSION)).thenReturn(aStateMachine().build());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setOrchestrationId(WORKFLOW_ID);

    when(stateMachineExecutionSimulator.getInfrastructureRequiredEntityType(
             APP_ID, Lists.newArrayList(SERVICE_INSTANCE_ID)))
        .thenReturn(Sets.newHashSet(EntityType.SSH_USER, EntityType.SSH_PASSWORD));

    RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
    assertThat(required).isNotNull().hasFieldOrPropertyWithValue(
        "entityTypes", workflow.getOrchestrationWorkflow().getRequiredEntityTypes());
  }

  /**
   * Should throw workflowType is null
   */
  @Test
  public void shouldThrowWorkflowNull() {
    try {
      ExecutionArgs executionArgs = new ExecutionArgs();

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.GENERAL_ERROR.getCode());
      assertThat(exception.getParams()).containsEntry("args", "workflowType");
    }
  }

  /**
   * Should throw orchestrationId is null for an orchestrated execution.
   */
  @Test
  public void shouldThrowNullOrchestrationId() {
    try {
      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.GENERAL_ERROR.getCode());
      assertThat(exception.getParams()).containsEntry("args", "orchestrationId");
    }
  }

  /*
   * Should throw invalid orchestration
   */
  @Test
  public void shouldThrowInvalidOrchestration() {
    try {
      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
      executionArgs.setOrchestrationId(WORKFLOW_ID);

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).containsEntry("message", "OrchestrationWorkflow not found");
    }
  }

  /*
   * Should throw Associated state machine not found
   */
  @Test
  public void shouldThrowNoStateMachine() {
    try {
      when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
      executionArgs.setOrchestrationId(WORKFLOW_ID);

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).containsEntry("message", "Associated state machine not found");
    }
  }

  /**
   * Should throw Null Service Id
   */
  @Test
  public void shouldThrowNoServiceId() {
    try {
      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.SIMPLE);

      String commandName = "Start";
      executionArgs.setCommandName(commandName);

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).containsEntry("message", "serviceId is null for a simple execution");
    }
  }

  /**
   * Should throw Null Service Id
   */
  @Test
  public void shouldThrowNoInstances() {
    try {
      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.SIMPLE);
      String serviceId = getUuid();
      executionArgs.setServiceId(serviceId);

      String commandName = "Start";
      executionArgs.setCommandName(commandName);

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).containsEntry("message", "serviceInstances are empty for a simple execution");
    }
  }
}
