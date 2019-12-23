package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static io.harness.rule.OwnerRule.YOGESH_CHAUHAN;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateMachine.StateMachineBuilder.aStateMachine;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.DEFAULT_VERSION;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
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
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.app.GeneralNotifyEventListener;
import software.wings.beans.Account;
import software.wings.beans.Account.Builder;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder;
import software.wings.beans.EntityType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.security.UserThreadLocal;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachineExecutionSimulator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class workflowExecutionServiceTest.
 *
 * @author Rishi
 */
@Listeners(GeneralNotifyEventListener.class)
public class WorkflowExecutionServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private WorkflowExecutionService workflowExecutionService;
  @InjectMocks
  private WorkflowExecutionServiceImpl workflowExecutionServiceSpy = spy(WorkflowExecutionServiceImpl.class);

  @Mock private WingsPersistence wingsPersistence;
  @Mock private WorkflowService workflowService;
  @Mock private PipelineService pipelineService;

  @Mock private ServiceResourceService serviceResourceServiceMock;
  @Mock private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Mock private MorphiaIterator<WorkflowExecution, WorkflowExecution> executionIterator;
  @Mock private DBCursor dbCursor;
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock WorkflowExecutionServiceHelper workflowExecutionServiceHelper;

  @Inject private WingsPersistence wingsPersistence1;

  private Workflow workflow =
      aWorkflow()
          .uuid(WORKFLOW_ID)
          .appId(APP_ID)
          .name(WORKFLOW_NAME)
          .defaultVersion(DEFAULT_VERSION)
          .orchestrationWorkflow(
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
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
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
   * Required execution args for orchestrated workflow.
   */
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowWorkflowNull() {
    assertThatThrownBy(() -> {
      ExecutionArgs executionArgs = new ExecutionArgs();
      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    })
        .isInstanceOf(GeneralException.class);
  }

  /**
   * Should throw orchestrationId is null for an orchestrated execution.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowNullOrchestrationId() {
    assertThatThrownBy(() -> {
      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    })
        .isInstanceOf(GeneralException.class);
  }

  /*
   * Should throw invalid orchestration
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowInvalidOrchestration() {
    try {
      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
      executionArgs.setOrchestrationId(WORKFLOW_ID);

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage("OrchestrationWorkflow not found");
      assertThat(exception.getParams()).containsEntry("message", "OrchestrationWorkflow not found");
    }
  }

  /*
   * Should throw Associated state machine not found
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowNoStateMachine() {
    try {
      when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
      executionArgs.setOrchestrationId(WORKFLOW_ID);

      RequiredExecutionArgs required = workflowExecutionService.getRequiredExecutionArgs(APP_ID, ENV_ID, executionArgs);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage("Associated state machine not found");
      assertThat(exception.getParams()).containsEntry("message", "Associated state machine not found");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void shouldFetchWorkflowExecution() {
    when(query.order(Sort.descending(anyString()))).thenReturn(query);
    when(query.get(any(FindOptions.class))).thenReturn(WorkflowExecution.builder().appId(APP_ID).build());
    WorkflowExecution workflowExecution =
        workflowExecutionService.fetchWorkflowExecution(APP_ID, asList(SERVICE_ID), asList(ENV_ID), WORKFLOW_ID);
    assertThat(workflowExecution).isNotNull();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
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
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
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
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
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
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
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
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
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

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testFetchApprovalStateExecutionDataWithEmptyStateExecution() {
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

    Map<String, StateExecutionData> hashMap = new HashMap();
    hashMap.put("Approval", approvalStateExecutionData);
    final StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance().displayName("Approval").stateExecutionMap(hashMap).build();

    PageResponse<StateExecutionInstance> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(stateExecutionInstance));
    pageResponse.setTotal(1l);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(statequery.get()).thenReturn(stateExecutionInstance);
    when(statequery.filter(any(), any())).thenReturn(statequery);
    when(wingsPersistence.createQuery(StateExecutionInstance.class)).thenReturn(statequery);
    when(wingsPersistence.query(eq(StateExecutionInstance.class), any(PageRequest.class))).thenReturn(pageResponse);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, workflowExecution.getUuid()))
        .thenReturn(workflowExecution);

    ApprovalStateExecutionData returnedExecutionData =
        workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
            APP_ID, workflowExecution.getUuid(), null, approvalDetails);
    assertThat(returnedExecutionData.getApprovalId()).isEqualTo(approvalId);
    assertThat(returnedExecutionData.getUserGroups()).isEqualTo(asList(userGroup.getUuid()));
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
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
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
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
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
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
    when(stateExecutionInstance.fetchStateExecutionData()).thenReturn(approvalStateExecutionData);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, workflowExecution.getUuid()))
        .thenReturn(workflowExecution);

    ApprovalStateExecutionData returnedExecutionData =
        workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
            APP_ID, workflowExecution.getUuid(), generateUuid(), approvalDetails);
    assertThat(returnedExecutionData.getApprovalId()).isEqualTo(approvalId);
    assertThat(returnedExecutionData.getUserGroups()).isEqualTo(asList(userGroup.getUuid()));
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
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
    when(stateExecutionInstance.fetchStateExecutionData()).thenReturn(null);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, workflowExecution.getUuid()))
        .thenReturn(workflowExecution);

    workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
        APP_ID, workflowExecution.getUuid(), generateUuid(), approvalDetails);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testInstancesDeployedFromExecution() {
    WorkflowExecution workflowExecution = createNewWorkflowExecution();
    int instancesDeployed = workflowExecutionService.getInstancesDeployedFromExecution(workflowExecution);
    assertThat(instancesDeployed).isEqualTo(1);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFetchDeploymentMetadataFFOn() {
    validateFetchDeploymentMetadata(false, true);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFetchDeploymentMetadataPipelineFFOn() {
    validateFetchDeploymentMetadata(true, true);
  }

  private void validateFetchDeploymentMetadata(boolean isPipeline, boolean ffOn) {
    when(featureFlagService.isEnabled(eq(FeatureName.ARTIFACT_STREAM_REFACTOR), any())).thenReturn(ffOn);
    ExecutionArgs executionArgs = new ExecutionArgs();
    if (isPipeline) {
      executionArgs.setWorkflowType(WorkflowType.PIPELINE);
      executionArgs.setPipelineId(PIPELINE_ID);
    } else {
      executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
      executionArgs.setOrchestrationId(WORKFLOW_ID);
    }
    Workflow workflow = aWorkflow().build();
    DeploymentMetadata deploymentMetadata =
        DeploymentMetadata.builder()
            .artifactVariables(singletonList(ArtifactVariable.builder().name("art1").build()))
            .artifactRequiredServiceIds(singletonList(SERVICE_ID))
            .build();
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.fetchDeploymentMetadata(APP_ID, workflow, null, null, null, false, null))
        .thenReturn(deploymentMetadata);
    when(pipelineService.fetchDeploymentMetadata(
             APP_ID, executionArgs.getPipelineId(), executionArgs.getWorkflowVariables(), null, null, false, null))
        .thenReturn(deploymentMetadata);
    DeploymentMetadata finalDeploymentMetadata =
        workflowExecutionService.fetchDeploymentMetadata(APP_ID, executionArgs);
    assertThat(finalDeploymentMetadata).isNotNull();
    assertThat(finalDeploymentMetadata.getArtifactVariables()).isNotNull();
    assertThat(finalDeploymentMetadata.getArtifactVariables().size()).isEqualTo(1);
    assertThat(finalDeploymentMetadata.getArtifactVariables().get(0).getName()).isEqualTo("art1");
    if (ffOn) {
      verify(serviceResourceService, never()).fetchServicesByUuids(any(), any());
    } else {
      verify(serviceResourceService).fetchServicesByUuids(any(), any());
    }
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFetchWorkflowVariables() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setOrchestrationId(WORKFLOW_ID);
    workflowExecutionService.fetchWorkflowVariables(APP_ID, executionArgs, null);
    verify(workflowExecutionServiceHelper).fetchWorkflowVariables(APP_ID, executionArgs, null);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testInstancesDeployedFromPipelineExecution() {
    WorkflowExecution workflowExecution = createNewWorkflowExecution();
    PipelineStageExecution pipelineStageExecution =
        PipelineStageExecution.builder().workflowExecutions(asList(workflowExecution)).build();

    PipelineExecution pipelineExecution = PipelineExecution.Builder.aPipelineExecution()
                                              .withWorkflowType(WorkflowType.PIPELINE)
                                              .withPipelineStageExecutions(asList(pipelineStageExecution))
                                              .build();
    WorkflowExecution parentWorkflowExecution =
        WorkflowExecution.builder().pipelineExecution(pipelineExecution).workflowType(WorkflowType.PIPELINE).build();
    int instancesDeployed = workflowExecutionService.getInstancesDeployedFromExecution(parentWorkflowExecution);
    assertThat(instancesDeployed).isEqualTo(1);
  }

  @Test
  @Owner(developers = YOGESH_CHAUHAN)
  @Category(UnitTests.class)
  public void shouldNotExecuteRollingWorkflowWithK8sV1Service() {
    Service service = Service.builder()
                          .uuid(SERVICE_ID)
                          .appId(APP_ID)
                          .accountId(ACCOUNT_ID)
                          .deploymentType(DeploymentType.KUBERNETES)
                          .isK8sV2(true)
                          .build();
    Workflow workflow =
        aWorkflow()
            .appId(APP_ID)
            .accountId(ACCOUNT_ID)
            .serviceId(SERVICE_ID)
            .services(asList(service))
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow().withOrchestrationWorkflowType(OrchestrationWorkflowType.ROLLING).build())
            .build();

    when(workflowService.getResolvedServices(any(), any())).thenReturn(asList(service));

    ExecutionArgs executionArgs = new ExecutionArgs();
    workflowExecutionServiceSpy.validateWorkflowTypeAndService(workflow, executionArgs);

    service.setK8sV2(false);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> workflowExecutionServiceSpy.validateWorkflowTypeAndService(workflow, executionArgs));
  }

  private PipelineExecution createPipelineExecution(ApprovalStateExecutionData approvalStateExecutionData) {
    PipelineStageExecution pipelineStageExecution = PipelineStageExecution.builder()
                                                        .status(ExecutionStatus.PAUSED)
                                                        .stateExecutionData(approvalStateExecutionData)
                                                        .build();
    return aPipelineExecution()
        .withPipelineStageExecutions(com.google.common.collect.Lists.newArrayList(pipelineStageExecution))
        .build();
  }

  private User createUser(String userId) {
    Account account = Builder.anAccount()
                          .withUuid(ACCOUNT_ID)
                          .withCompanyName(COMPANY_NAME)
                          .withAccountName(ACCOUNT_NAME)
                          .withAuthenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
                          .build();
    return anUser()
        .withUuid(userId)
        .withAppId(APP_ID)
        .withEmailVerified(true)
        .withEmail(USER_EMAIL)
        .withAccounts(asList(account))
        .build();
  }

  private UserGroup createUserGroup(List<String> memberIds) {
    return UserGroup.builder().accountId(ACCOUNT_ID).uuid(USER_GROUP_ID).memberIds(memberIds).build();
  }

  private WorkflowExecution createNewWorkflowExecution() {
    return WorkflowExecution.builder()
        .appId(APP_ID)
        .appName(APP_NAME)
        .envType(NON_PROD)
        .status(ExecutionStatus.PAUSED)
        .workflowType(WorkflowType.ORCHESTRATION)
        .uuid(generateUuid())
        .serviceExecutionSummaries(
            asList(ElementExecutionSummaryBuilder.anElementExecutionSummary()
                       .withInstanceStatusSummaries(asList(
                           anInstanceStatusSummary()
                               .withInstanceElement(
                                   InstanceElement.Builder.anInstanceElement().uuid("id1").podName("pod").build())
                               .build()))
                       .build()))
        .build();
  }

  private void saveUserToPersistence(User user) {
    wingsPersistence1.save(user);
  }

  private void saveUserGroupToPersistence(UserGroup userGroup) {
    wingsPersistence1.save(userGroup);
  }
}
