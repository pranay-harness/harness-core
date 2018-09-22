package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.time.EpochUtils.PST_ZONE_ID;
import static io.harness.time.EpochUtils.calculateEpochMilliOfStartOfDayForXDaysInPastFromNow;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.StateMachine.StateMachineBuilder.aStateMachine;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.DEFAULT_VERSION;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_GROUP_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.mongodb.DBCursor;
import com.mongodb.WriteResult;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.WingsBaseTest;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.EntityType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.ServiceInstance;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.command.Command;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.waitnotify.NotifyEventListener;

import java.util.List;

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
  @Mock private MorphiaIterator<WorkflowExecution, WorkflowExecution> executionIterator;
  @Mock private DBCursor dbCursor;
  @Mock private AppService appService;

  @Inject private WingsPersistence wingsPersistence1;

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
  @Mock Query<StateExecutionInstance> statequery;
  @Mock StateExecutionInstance stateExecutionInstance;

  /**
   * test setup.
   */
  @Before
  public void setUp() {
    when(wingsPersistence.createQuery(eq(WorkflowExecution.class))).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.in(any())).thenReturn(query);
    when(end.greaterThanOrEq(any())).thenReturn(query);
    when(end.hasAnyOf(any())).thenReturn(query);
    when(end.doesNotExist()).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);

    when(wingsPersistence.createUpdateOperations(WorkflowExecution.class)).thenReturn(updateOperations);
    when(updateOperations.set(anyString(), any())).thenReturn(updateOperations);
    when(updateOperations.addToSet(anyString(), any())).thenReturn(updateOperations);
    when(wingsPersistence.update(query, updateOperations)).thenReturn(updateResults);
    when(updateResults.getWriteResult()).thenReturn(writeResult);
    when(writeResult.getN()).thenReturn(1);

    when(wingsPersistence.createQuery(eq(StateExecutionInstance.class))).thenReturn(statequery);
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

    ServiceInstance inst1 = ServiceInstance.Builder.aServiceInstance().withUuid(generateUuid()).build();
    ServiceInstance inst2 = ServiceInstance.Builder.aServiceInstance().withUuid(generateUuid()).build();
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
      assertThat(exception).hasMessage(ErrorCode.GENERAL_ERROR.name());
      assertThat(exception.getParams()).containsEntry("message", "workflowType");
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
      assertThat(exception).hasMessage(ErrorCode.GENERAL_ERROR.name());
      assertThat(exception.getParams()).containsEntry("message", "orchestrationId");
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
      assertThat(exception).hasMessage(INVALID_REQUEST.name());
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
      assertThat(exception).hasMessage(INVALID_REQUEST.name());
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
      assertThat(exception).hasMessage(INVALID_REQUEST.name());
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
      String serviceId = generateUuid();
      executionArgs.setServiceId(serviceId);

      String commandName = "Start";
      executionArgs.setCommandName(commandName);

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(INVALID_REQUEST.name());
      assertThat(exception.getParams()).containsEntry("message", "serviceInstances are empty for a simple execution");
    }
  }

  @Test
  public void shouldTestWorkflowExecutionIterator() {
    when(query.fetch()).thenReturn(executionIterator);
    long fromDateEpochMilli = calculateEpochMilliOfStartOfDayForXDaysInPastFromNow(30, PST_ZONE_ID);
    HIterator<WorkflowExecution> executionIterator =
        workflowExecutionService.obtainWorkflowExecutionIterator(asList(APP_ID), fromDateEpochMilli);
    assertNotNull(executionIterator);
  }

  @Test
  public void shouldTestGetWorkflowExecutionsByIterator() {
    when(query.fetch()).thenReturn(executionIterator);
    when(executionIterator.getCursor()).thenReturn(dbCursor);
    WorkflowExecution workflowExecution1 =
        aWorkflowExecution().withAppId(APP_ID).withAppName(APP_NAME).withEnvType(PROD).withStatus(SUCCESS).build();

    WorkflowExecution workflowExecution2 = aWorkflowExecution()
                                               .withAppId(APP_ID)
                                               .withAppName(APP_NAME)
                                               .withEnvType(NON_PROD)
                                               .withStatus(ExecutionStatus.FAILED)
                                               .build();

    when(executionIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);

    when(executionIterator.next()).thenReturn(workflowExecution1).thenReturn(workflowExecution2);

    long fromDateEpochMilli = calculateEpochMilliOfStartOfDayForXDaysInPastFromNow(30, PST_ZONE_ID);
    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.obtainWorkflowExecutions(asList(APP_ID), fromDateEpochMilli);
    assertNotNull(workflowExecutions);
    Assertions.assertThat(workflowExecutions).hasSize(2);
  }

  @Test
  public void shouldFetchWorkflowExecution() {
    when(query.order(Sort.descending(anyString()))).thenReturn(query);
    when(query.get(any(FindOptions.class))).thenReturn(aWorkflowExecution().withAppId(APP_ID).build());
    WorkflowExecution workflowExecution =
        workflowExecutionService.fetchWorkflowExecution(APP_ID, asList(SERVICE_ID), asList(ENV_ID), WORKFLOW_ID);
    assertThat(workflowExecution).isNotNull();
  }

  @Test
  public void testRejectWithUserGroup() {
    String approvalId = generateUuid();
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setApprovalId(approvalId);
    approvalDetails.setAction(Action.REJECT);

    User user = createUser(USER_ID);
    saveUserToPersistence(user);
    UserGroup userGroup = createUserGroup(asList(user.getUuid()));
    saveUserGroupToPersistence(userGroup);

    UserThreadLocal.set(user);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    boolean success =
        workflowExecutionService.approveOrRejectExecution(APP_ID, asList(userGroup.getUuid()), approvalDetails);
    assertThat(success).isEqualTo(true);
  }

  @Test
  public void testApproveWithUserGroup() {
    String approvalId = generateUuid();
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setApprovalId(approvalId);
    approvalDetails.setAction(Action.APPROVE);

    User user = createUser(USER_ID);
    saveUserToPersistence(user);
    UserGroup userGroup = createUserGroup(asList(user.getUuid()));
    saveUserGroupToPersistence(userGroup);

    UserThreadLocal.set(user);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    boolean success =
        workflowExecutionService.approveOrRejectExecution(APP_ID, asList(userGroup.getUuid()), approvalDetails);
    assertThat(success).isEqualTo(true);
  }

  @Test(expected = InvalidRequestException.class)
  public void testRejectWithUserGroupException() {
    String approvalId = generateUuid();
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setApprovalId(approvalId);
    approvalDetails.setAction(Action.REJECT);

    User user = createUser(USER_ID);
    User user1 = createUser(USER_ID + "1");
    saveUserToPersistence(user);
    saveUserToPersistence(user1);
    UserGroup userGroup = createUserGroup(asList(user.getUuid()));
    saveUserGroupToPersistence(userGroup);

    UserThreadLocal.set(user1);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    workflowExecutionService.approveOrRejectExecution(APP_ID, asList(userGroup.getUuid()), approvalDetails);
  }

  @Test(expected = InvalidRequestException.class)
  public void testApproveWithUserGroupException() {
    String approvalId = generateUuid();
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setApprovalId(approvalId);
    approvalDetails.setAction(Action.APPROVE);

    User user = createUser(USER_ID);
    User user1 = createUser(USER_ID + "1");
    saveUserToPersistence(user);
    saveUserToPersistence(user1);
    UserGroup userGroup = createUserGroup(asList(user.getUuid()));
    saveUserGroupToPersistence(userGroup);

    UserThreadLocal.set(user1);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    workflowExecutionService.approveOrRejectExecution(APP_ID, asList(userGroup.getUuid()), approvalDetails);
  }

  @Test
  public void testFetchApprovalStateExecutionDataForPipeline() {
    String approvalId = generateUuid();
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setApprovalId(approvalId);
    approvalDetails.setAction(Action.APPROVE);

    User user = createUser(USER_ID);
    UserGroup userGroup = createUserGroup(asList(user.getUuid()));

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId(approvalId).userGroups(asList(userGroup.getUuid())).build();
    approvalStateExecutionData.setStatus(ExecutionStatus.PAUSED);
    WorkflowExecution workflowExecution = createNewWorkflowExecution();
    workflowExecution.setWorkflowType(WorkflowType.PIPELINE);
    workflowExecution.setPipelineExecution(createPipelineExecution(approvalStateExecutionData));

    when(workflowExecutionService.getWorkflowExecution(APP_ID, workflowExecution.getUuid()))
        .thenReturn(workflowExecution);

    ApprovalStateExecutionData returnedExecutionData =
        workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
            APP_ID, workflowExecution.getUuid(), null, approvalDetails);
    assertThat(returnedExecutionData.getApprovalId()).isEqualTo(approvalId);
    assertThat(returnedExecutionData.getUserGroups()).isEqualTo(asList(userGroup.getUuid()));
  }

  @Test(expected = WingsException.class)
  public void testFetchApprovalStateExecutionDataForPipelineWithException() {
    String approvalId = generateUuid();
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setApprovalId(approvalId);
    approvalDetails.setAction(Action.APPROVE);

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    WorkflowExecution workflowExecution = createNewWorkflowExecution();
    workflowExecution.setWorkflowType(WorkflowType.PIPELINE);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, workflowExecution.getUuid()))
        .thenReturn(workflowExecution);

    workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
        APP_ID, workflowExecution.getUuid(), null, approvalDetails);
  }

  @Test(expected = WingsException.class)
  public void testFetchApprovalStateExecutionDataForPipelineWithNoApprovalDataException() {
    String approvalId = generateUuid();
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setApprovalId(approvalId);
    approvalDetails.setAction(Action.APPROVE);

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    WorkflowExecution workflowExecution = createNewWorkflowExecution();
    workflowExecution.setWorkflowType(WorkflowType.PIPELINE);
    workflowExecution.setPipelineExecution(createPipelineExecution(null));

    when(workflowExecutionService.getWorkflowExecution(APP_ID, workflowExecution.getUuid()))
        .thenReturn(workflowExecution);

    workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
        APP_ID, workflowExecution.getUuid(), null, approvalDetails);
  }

  @Test
  public void testFetchApprovalStateExecutionDataForWorkflow() {
    String approvalId = generateUuid();
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setApprovalId(approvalId);
    approvalDetails.setAction(Action.APPROVE);

    User user = createUser(USER_ID);
    UserGroup userGroup = createUserGroup(asList(user.getUuid()));

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId(approvalId).userGroups(asList(userGroup.getUuid())).build();
    approvalStateExecutionData.setStatus(ExecutionStatus.PAUSED);
    WorkflowExecution workflowExecution = createNewWorkflowExecution();

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(statequery.get()).thenReturn(stateExecutionInstance);
    when(statequery.filter(any(), any())).thenReturn(statequery);
    when(wingsPersistence.createQuery(StateExecutionInstance.class)).thenReturn(statequery);
    when(stateExecutionInstance.getStateExecutionData()).thenReturn(approvalStateExecutionData);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, workflowExecution.getUuid()))
        .thenReturn(workflowExecution);

    ApprovalStateExecutionData returnedExecutionData =
        workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
            APP_ID, workflowExecution.getUuid(), generateUuid(), approvalDetails);
    assertThat(returnedExecutionData.getApprovalId()).isEqualTo(approvalId);
    assertThat(returnedExecutionData.getUserGroups()).isEqualTo(asList(userGroup.getUuid()));
  }

  @Test(expected = WingsException.class)
  public void testFetchApprovalStateExecutionDataForWorkflowWithNoApprovalDataException() {
    String approvalId = generateUuid();
    ApprovalDetails approvalDetails = new ApprovalDetails();
    approvalDetails.setApprovalId(approvalId);
    approvalDetails.setAction(Action.APPROVE);

    WorkflowExecution workflowExecution = createNewWorkflowExecution();

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(statequery.get()).thenReturn(stateExecutionInstance);
    when(statequery.filter(any(), any())).thenReturn(statequery);
    when(wingsPersistence.createQuery(StateExecutionInstance.class)).thenReturn(statequery);
    when(stateExecutionInstance.getStateExecutionData()).thenReturn(null);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, workflowExecution.getUuid()))
        .thenReturn(workflowExecution);

    workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
        APP_ID, workflowExecution.getUuid(), generateUuid(), approvalDetails);
  }

  private PipelineExecution createPipelineExecution(ApprovalStateExecutionData approvalStateExecutionData) {
    PipelineStageExecution pipelineStageExecution = PipelineStageExecution.Builder.aPipelineStageExecution()
                                                        .withStatus(ExecutionStatus.PAUSED)
                                                        .withStateExecutionData(approvalStateExecutionData)
                                                        .build();
    return PipelineExecution.Builder.aPipelineExecution()
        .withPipelineStageExecutions(com.google.common.collect.Lists.newArrayList(pipelineStageExecution))
        .build();
  }

  private User createUser(String userId) {
    return anUser().withUuid(userId).withAppId(APP_ID).withEmailVerified(true).withEmail(USER_EMAIL).build();
  }

  private UserGroup createUserGroup(List<String> memberIds) {
    return UserGroup.builder().accountId(ACCOUNT_ID).uuid(USER_GROUP_ID).memberIds(memberIds).build();
  }

  private WorkflowExecution createNewWorkflowExecution() {
    return aWorkflowExecution()
        .withAppId(APP_ID)
        .withAppName(APP_NAME)
        .withEnvType(NON_PROD)
        .withStatus(ExecutionStatus.PAUSED)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withUuid(generateUuid())
        .build();
  }

  private void saveUserToPersistence(User user) {
    wingsPersistence1.save(user);
  }

  private void saveUserGroupToPersistence(UserGroup userGroup) {
    wingsPersistence1.save(userGroup);
  }
}
