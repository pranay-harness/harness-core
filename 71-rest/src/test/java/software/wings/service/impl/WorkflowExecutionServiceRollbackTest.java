package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.PRASHANT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static software.wings.api.CloudProviderType.PCF;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.FeatureName.INFRA_MAPPING_REFACTOR;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.sm.StateMachine.StateMachineBuilder.aStateMachine;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.DEFAULT_VERSION;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder;
import software.wings.beans.EntityType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.WingsPersistence;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.deployment.checks.AccountExpirationChecker;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.RollbackConfirmation;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.rollback.RollbackStateMachineGenerator;

import java.util.Collections;

public class WorkflowExecutionServiceRollbackTest extends WingsBaseTest {
  @InjectMocks @Inject private WorkflowExecutionService workflowExecutionService;

  @Inject private WingsPersistence wingsPersistence;
  @Mock private WorkflowService workflowService;
  @Mock private InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private AppService appService;
  @Mock private WorkflowExecutionServiceHelper workflowExecutionServiceHelper;
  @Mock private AccountExpirationChecker accountExpirationChecker;
  @Mock private RollbackStateMachineGenerator rollbackStateMachineGenerator;
  @Mock private StateMachineExecutor stateMachineExecutor;
  @Mock private ArtifactService artifactService;

  private Workflow workflow;
  @Before
  public void setup() {
    workflow = aWorkflow()
                   .uuid(WORKFLOW_ID)
                   .appId(APP_ID)
                   .name(WORKFLOW_NAME)
                   .defaultVersion(DEFAULT_VERSION)
                   .orchestrationWorkflow(
                       aCanaryOrchestrationWorkflow()
                           .withRequiredEntityTypes(Sets.newHashSet(EntityType.SSH_USER, EntityType.SSH_PASSWORD))
                           .build())
                   .build();
    InfrastructureDefinition pcfInfraDef = InfrastructureDefinition.builder()
                                               .uuid(INFRA_DEFINITION_ID)
                                               .appId(APP_ID)
                                               .envId(ENV_ID)
                                               .cloudProviderType(PCF)
                                               .deploymentType(DeploymentType.PCF)
                                               .build();
    when(infrastructureDefinitionService.get(any(), any())).thenReturn(pcfInfraDef);

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(appService.get(APP_ID)).thenReturn(anApplication().build());
  }
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testOnDemandRollbackConfirmationAlreadyRunningExecution() {
    WorkflowExecution pausedWE = createNewWorkflowExecution(false);
    pausedWE.setStatus(PAUSED);
    wingsPersistence.save(pausedWE);

    WorkflowExecution newWe = createNewWorkflowExecution(false);
    RollbackConfirmation rollbackConfirmation = workflowExecutionService.getOnDemandRollbackConfirmation(APP_ID, newWe);
    assertThat(rollbackConfirmation).isNotNull();
    assertThat(rollbackConfirmation.isValid()).isFalse();
    assertThat(rollbackConfirmation.getValidationMessage())
        .isEqualTo("Cannot trigger Rollback, active execution found");
    assertThat(rollbackConfirmation.getActiveWorkflowExecution().getUuid()).isEqualTo(pausedWE.getUuid());
  }

  private WorkflowExecution createNewWorkflowExecution(boolean rollback) {
    WorkflowExecutionBuilder executionBuilder =
        WorkflowExecution.builder()
            .appId(APP_ID)
            .appName(APP_NAME)
            .envType(PROD)
            .status(SUCCESS)
            .serviceIds(Collections.singletonList(SERVICE_ID))
            .workflowType(WorkflowType.ORCHESTRATION)
            .workflowId(WORKFLOW_ID)
            .infraMappingIds(Collections.singletonList(INFRA_MAPPING_ID))
            .infraDefinitionIds(Collections.singletonList(INFRA_DEFINITION_ID))
            .uuid(generateUuid())
            .serviceExecutionSummaries(
                asList(ElementExecutionSummaryBuilder.anElementExecutionSummary()
                           .withInstanceStatusSummaries(asList(
                               anInstanceStatusSummary()
                                   .withInstanceElement(
                                       InstanceElement.Builder.anInstanceElement().uuid("id1").podName("pod").build())
                                   .build()))
                           .build()));
    return rollback ? executionBuilder.onDemandRollback(true).build() : executionBuilder.build();
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testOnDemandRollbackConfirmationOnRolledBackExecution() {
    WorkflowExecution workflowExecution = createNewWorkflowExecution(false);
    workflowExecution.setUuid(WORKFLOW_EXECUTION_ID);
    workflowExecution.setOnDemandRollback(true);

    RollbackConfirmation rollbackConfirmation =
        workflowExecutionService.getOnDemandRollbackConfirmation(APP_ID, workflowExecution);
    assertThat(rollbackConfirmation).isNotNull();
    assertThat(rollbackConfirmation.isValid()).isFalse();
    assertThat(rollbackConfirmation.getValidationMessage())
        .isEqualTo("Cannot trigger Rollback for RolledBack execution");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testOnDemandRollbackConfirmationExceptions() {
    WorkflowExecution savedWE = createNewWorkflowExecution(false);
    savedWE.setStatus(SUCCESS);
    savedWE.setName("test");
    wingsPersistence.save(savedWE);

    WorkflowExecution newWE = createNewWorkflowExecution(false);
    newWE.setName("test");

    try {
      workflowExecutionService.getOnDemandRollbackConfirmation(APP_ID, newWE);
      failBecauseExceptionWasNotThrown(InvalidRequestException.class);
    } catch (Exception e) {
      assertThat(e).hasMessage("This is not the latest successful workflowExecution: test");
    }

    try {
      workflowExecutionService.getOnDemandRollbackConfirmation(APP_ID, savedWE);
      failBecauseExceptionWasNotThrown(InvalidRequestException.class);
    } catch (Exception e) {
      assertThat(e).hasMessage("No previous execution before this execution to rollback to, workflowExecution: test");
    }
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testOnDemandRollbackConfirmationNoPreviousArtifact() {
    WorkflowExecution previousWE = createNewWorkflowExecution(false);
    previousWE.setArtifacts(Collections.emptyList());
    previousWE.setName("test");

    wingsPersistence.save(previousWE);

    WorkflowExecution newWE = createNewWorkflowExecution(false);
    wingsPersistence.save(newWE);

    try {
      workflowExecutionService.getOnDemandRollbackConfirmation(APP_ID, newWE);
      failBecauseExceptionWasNotThrown(InvalidRequestException.class);
    } catch (Exception e) {
      assertThat(e).hasMessage("No artifact found in previous execution");
    }
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testOnDemandRollbackConfirmationMultiService() {
    WorkflowExecution previousWE = createNewWorkflowExecution(false);
    previousWE.setName("test");
    wingsPersistence.save(previousWE);

    WorkflowExecution newWE = createNewWorkflowExecution(false);
    newWE.setServiceIds(asList(SERVICE_ID, SERVICE_ID + "_1"));
    wingsPersistence.save(newWE);

    try {
      workflowExecutionService.getOnDemandRollbackConfirmation(APP_ID, newWE);
      failBecauseExceptionWasNotThrown(InvalidRequestException.class);
    } catch (Exception e) {
      assertThat(e).hasMessage("Rollback Execution is not available for multi Service workflow");
    }
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testOnDemandRollbackConfirmationSuccess() {
    WorkflowExecution previousWE = createNewWorkflowExecution(false);
    previousWE.setArtifacts(Collections.emptyList());
    previousWE.setName("test");
    Artifact artifact = anArtifact().build();
    previousWE.setArtifacts(Collections.singletonList(artifact));

    wingsPersistence.save(previousWE);

    WorkflowExecution newWE = createNewWorkflowExecution(false);
    wingsPersistence.save(newWE);

    RollbackConfirmation rollbackConfirmation = workflowExecutionService.getOnDemandRollbackConfirmation(APP_ID, newWE);
    assertThat(rollbackConfirmation).isNotNull();
    assertThat(rollbackConfirmation.isValid()).isTrue();
    assertThat(rollbackConfirmation.getArtifacts()).isEqualTo(Collections.singletonList(artifact));
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testGetOnDemandRollbackAvailable() {
    WorkflowExecution lastSuccessfulWE = createNewWorkflowExecution(false);
    lastSuccessfulWE.setStatus(ExecutionStatus.ABORTED);

    boolean result = workflowExecutionService.getOnDemandRollbackAvailable(APP_ID, lastSuccessfulWE);
    assertThat(result).isFalse();

    lastSuccessfulWE.setStatus(SUCCESS);
    lastSuccessfulWE.setWorkflowType(WorkflowType.PIPELINE);

    result = workflowExecutionService.getOnDemandRollbackAvailable(APP_ID, lastSuccessfulWE);
    assertThat(result).isFalse();

    lastSuccessfulWE.setWorkflowType(WorkflowType.ORCHESTRATION);
    lastSuccessfulWE.setEnvType(NON_PROD);
    result = workflowExecutionService.getOnDemandRollbackAvailable(APP_ID, lastSuccessfulWE);
    assertThat(result).isFalse();

    lastSuccessfulWE.setEnvType(PROD);
    lastSuccessfulWE.setInfraDefinitionIds(asList("id1", "id2"));
    result = workflowExecutionService.getOnDemandRollbackAvailable(APP_ID, lastSuccessfulWE);
    assertThat(result).isFalse();

    lastSuccessfulWE.setInfraDefinitionIds(Collections.singletonList("id2"));
    result = workflowExecutionService.getOnDemandRollbackAvailable(APP_ID, lastSuccessfulWE);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testTriggerRollbackExecutionWorkflow() {
    WorkflowExecution previousWE = createNewWorkflowExecution(false);
    previousWE.setArtifacts(Collections.emptyList());
    previousWE.setName("test");
    Artifact artifact = anArtifact().build();
    previousWE.setArtifacts(Collections.singletonList(artifact));

    wingsPersistence.save(previousWE);

    WorkflowExecution newWE = createNewWorkflowExecution(false);
    ExecutionArgs executionArgs = new ExecutionArgs();
    newWE.setExecutionArgs(executionArgs);
    newWE.setEnvId(ENV_ID);
    newWE.setStartTs(System.currentTimeMillis());
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setOrchestrationId(WORKFLOW_ID);
    wingsPersistence.save(newWE);

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, ACCOUNT_ID)).thenReturn(true);
    when(workflowExecutionServiceHelper.obtainWorkflow(APP_ID, WORKFLOW_ID, true)).thenReturn(workflow);
    doNothing().when(accountExpirationChecker).check(ACCOUNT_ID);

    StateMachine rollbackSM = aStateMachine().build();
    when(rollbackStateMachineGenerator.generateForRollbackExecution(APP_ID, newWE.getUuid(), true))
        .thenReturn(rollbackSM);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    WorkflowExecution rollbackWE = createNewWorkflowExecution(false);
    rollbackWE.setExecutionArgs(executionArgs);
    rollbackWE.setUuid(WORKFLOW_EXECUTION_ID);
    when(workflowExecutionServiceHelper.obtainExecution(workflow, rollbackSM, ENV_ID, null, executionArgs, true))
        .thenReturn(rollbackWE);
    when(workflowExecutionServiceHelper.obtainWorkflowStandardParams(APP_ID, ENV_ID, executionArgs, workflow))
        .thenReturn(aWorkflowStandardParams().build());
    when(artifactService.listByIds(any(), any())).thenReturn(Collections.singletonList(artifact));
    when(stateMachineExecutor.queue(any(), any())).thenReturn(new StateExecutionInstance());

    WorkflowExecution rollbackWEResult = workflowExecutionService.triggerRollbackExecutionWorkflow(APP_ID, newWE);
    assertThat(rollbackWEResult.isOnDemandRollback()).isTrue();
    assertThat(rollbackWEResult.getOriginalExecution().getExecutionId()).isEqualTo(newWE.getUuid());
    assertThat(rollbackWEResult.getOriginalExecution().getStartTs()).isEqualTo(newWE.getStartTs());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testCheckIfOnDemand() {
    WorkflowExecution rollbackExecution = createNewWorkflowExecution(true);
    wingsPersistence.save(rollbackExecution);
    assertThat(workflowExecutionService.checkIfOnDemand(APP_ID, rollbackExecution.getUuid())).isTrue();
  }
}
