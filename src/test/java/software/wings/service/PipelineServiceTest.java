package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.sm.ExecutionStatus.QUEUED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.PIPELINE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.PipelineServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.EnvState;
import software.wings.utils.WingsTestConstants;

import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/3/16.
 */
public class PipelineServiceTest extends WingsBaseTest {
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private WorkflowService workflowService;
  @Mock private AppService appService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ExecutorService executorService;
  @Mock private ArtifactService artifactService;
  @Mock private Query<PipelineExecution> query;
  @Mock private FieldEnd end;

  @Inject @InjectMocks private PipelineService pipelineService;

  @Spy @InjectMocks private PipelineService spyPipelineService = new PipelineServiceImpl();

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(PipelineExecution.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
    when(appService.get(APP_ID)).thenReturn(anApplication().withUuid(APP_ID).withName(APP_NAME).build());
  }

  /**
   * Should list pipeline executions.
   */
  @Test
  public void shouldListPipelineExecutions() {
    PipelineExecution pipelineExecution = aPipelineExecution().withStatus(ExecutionStatus.SUCCESS).build();
    PageResponse pageResponse = aPageResponse().withResponse(asList(pipelineExecution)).build();

    when(wingsPersistence.query(PipelineExecution.class, aPageRequest().build())).thenReturn(pageResponse);

    PageResponse<PipelineExecution> pipelineExecutions = pipelineService.listPipelineExecutions(aPageRequest().build());

    assertThat(pipelineExecutions.getResponse()).hasSize(1);
    assertThat(pipelineExecutions.getResponse()).containsOnly(pipelineExecution);
  }

  /**
   * Should execute.
   */
  @Test
  public void shouldExecute() {
    Artifact artifact = anArtifact().withUuid(ARTIFACT_ID).withDisplayName(ARTIFACT_NAME).build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(asList(artifact));
    when(workflowExecutionService.triggerPipelineExecution(APP_ID, PIPELINE_ID, executionArgs))
        .thenReturn(
            aWorkflowExecution().withUuid(PIPELINE_WORKFLOW_EXECUTION_ID).withStatus(ExecutionStatus.SUCCESS).build());
    when(artifactService.get(APP_ID, ARTIFACT_ID)).thenReturn(artifact);
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(aPipeline().withUuid(PIPELINE_ID).build());
    when(wingsPersistence.saveAndGet(eq(PipelineExecution.class), any()))
        .thenReturn(aPipelineExecution()
                        .withUuid(PIPELINE_EXECUTION_ID)
                        .withWorkflowExecutionId(PIPELINE_WORKFLOW_EXECUTION_ID)
                        .withStatus(ExecutionStatus.SUCCESS)
                        .build());
    doNothing().when(spyPipelineService).refreshPipelineExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);

    WorkflowExecution workflowExecution = spyPipelineService.execute(APP_ID, PIPELINE_ID, executionArgs);

    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(workflowExecution.getUuid()).isEqualTo(PIPELINE_WORKFLOW_EXECUTION_ID);
    verify(wingsPersistence).get(Pipeline.class, APP_ID, PIPELINE_ID);
    verify(wingsPersistence).saveAndGet(eq(PipelineExecution.class), any(PipelineExecution.class));
    verify(spyPipelineService).refreshPipelineExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);
  }

  /**
   * Should not refresh pipeline execution summary for finished execution.
   */
  @Test
  public void shouldNotRefreshPipelineExecutionSummaryForFinishedExecution() {
    when(query.get()).thenReturn(aPipelineExecution().withStatus(ExecutionStatus.SUCCESS).build());
    pipelineService.refreshPipelineExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);
    verify(wingsPersistence).createQuery(PipelineExecution.class);
    verifyNoMoreInteractions(workflowService, workflowExecutionService, wingsPersistence);
  }

  /**
   * Should refresh pipeline execution.
   */
  @Test
  public void shouldRefreshPipelineExecutionForAllQueuedStates() {
    StateMachine stateMachine = createPipelineStateMachine();
    when(workflowService.readLatest(APP_ID, PIPELINE_ID, null)).thenReturn(stateMachine);
    PageResponse pageResponse = PageResponse.Builder.aPageResponse().withResponse(asList()).build();
    when(wingsPersistence.query(eq(StateExecutionInstance.class), any(PageRequest.class))).thenReturn(pageResponse);
    PipelineExecution pipelineExecution = aPipelineExecution()
                                              .withAppId(APP_ID)
                                              .withWorkflowExecutionId(PIPELINE_WORKFLOW_EXECUTION_ID)
                                              .withPipelineId(PIPELINE_ID)
                                              .withStatus(RUNNING)
                                              .build();
    when(query.get()).thenReturn(pipelineExecution);
    when(workflowExecutionService.getExecutionDetails(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID))
        .thenReturn(aWorkflowExecution().withStatus(RUNNING).build());
    pipelineService.refreshPipelineExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);

    assertThat(pipelineExecution.getStatus()).isEqualTo(RUNNING);
    assertThat(pipelineExecution.getPipelineStageExecutions().size()).isEqualTo(3);
    pipelineExecution.getPipelineStageExecutions().forEach(
        pipelineStageExecution -> assertThat(pipelineStageExecution.getStatus()).isEqualTo(QUEUED));
  }

  /**
   * Should refresh pipeline execution for running state.
   */
  @Test
  public void shouldRefreshPipelineExecutionForRunningState() {
    StateMachine stateMachine = createPipelineStateMachine();
    when(workflowService.readLatest(APP_ID, PIPELINE_ID, null)).thenReturn(stateMachine);

    StateExecutionInstance seiEnvDev =
        aStateExecutionInstance()
            .withStatus(SUCCESS)
            .withStateType(ENV_STATE.name())
            .withStateName("DEV")
            .withStateExecutionMap(ImmutableMap.of(
                "DEV", anEnvStateExecutionData().withWorkflowExecutionId(WORKFLOW_EXECUTION_ID).build()))
            .build();
    StateExecutionInstance seiApproval =
        aStateExecutionInstance().withStatus(RUNNING).withStateType(APPROVAL.name()).withStateName("APPROVAL").build();

    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(aWorkflowExecution().withStatus(SUCCESS).build());

    PageResponse pageResponse =
        PageResponse.Builder.aPageResponse().withResponse(asList(seiEnvDev, seiApproval)).build();

    when(wingsPersistence.query(eq(StateExecutionInstance.class), any(PageRequest.class))).thenReturn(pageResponse);
    PipelineExecution pipelineExecution = aPipelineExecution()
                                              .withAppId(APP_ID)
                                              .withWorkflowExecutionId(PIPELINE_WORKFLOW_EXECUTION_ID)
                                              .withPipelineId(PIPELINE_ID)
                                              .withStatus(RUNNING)
                                              .build();
    when(query.get()).thenReturn(pipelineExecution);
    when(workflowExecutionService.getExecutionDetails(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID))
        .thenReturn(aWorkflowExecution().withStatus(RUNNING).build());
    pipelineService.refreshPipelineExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);

    assertThat(pipelineExecution.getStatus()).isEqualTo(RUNNING);
    assertThat(pipelineExecution.getPipelineStageExecutions().size()).isEqualTo(3);
    assertThat(pipelineExecution.getPipelineStageExecutions()
                   .stream()
                   .map(PipelineStageExecution::getStatus)
                   .collect(Collectors.toList()))
        .hasSameElementsAs(asList(SUCCESS, RUNNING, QUEUED));
    assertThat(pipelineExecution.getPipelineStageExecutions()
                   .stream()
                   .flatMap(pse -> pse.getWorkflowExecutions().stream())
                   .map(WorkflowExecution::getStatus)
                   .collect(Collectors.toList()))
        .hasSize(1)
        .isEqualTo(asList(SUCCESS));
  }

  /**
   * Should refresh pipeline execution for running state.
   */
  @Test
  public void shouldRefreshPipelineExecutionForCompletedExecution() {
    StateMachine stateMachine = createPipelineStateMachine();
    when(workflowService.readLatest(APP_ID, PIPELINE_ID, null)).thenReturn(stateMachine);

    StateExecutionInstance seiEnvDev =
        aStateExecutionInstance()
            .withStatus(SUCCESS)
            .withStateType(ENV_STATE.name())
            .withStateName("DEV")
            .withStateExecutionMap(ImmutableMap.of(
                "DEV", anEnvStateExecutionData().withWorkflowExecutionId(WORKFLOW_EXECUTION_ID).build()))
            .build();
    StateExecutionInstance seiApproval =
        aStateExecutionInstance().withStatus(SUCCESS).withStateType(APPROVAL.name()).withStateName("APPROVAL").build();
    StateExecutionInstance seiEnvProd =
        aStateExecutionInstance()
            .withStatus(SUCCESS)
            .withStateType(ENV_STATE.name())
            .withStateName("PROD")
            .withStateExecutionMap(ImmutableMap.of(
                "PROD", anEnvStateExecutionData().withWorkflowExecutionId(WORKFLOW_EXECUTION_ID).build()))
            .build();
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(aWorkflowExecution().withStatus(SUCCESS).build());

    PageResponse pageResponse =
        PageResponse.Builder.aPageResponse().withResponse(asList(seiEnvDev, seiApproval, seiEnvProd)).build();

    when(wingsPersistence.query(eq(StateExecutionInstance.class), any(PageRequest.class))).thenReturn(pageResponse);
    PipelineExecution pipelineExecution = aPipelineExecution()
                                              .withAppId(APP_ID)
                                              .withWorkflowExecutionId(PIPELINE_WORKFLOW_EXECUTION_ID)
                                              .withPipelineId(PIPELINE_ID)
                                              .withStatus(RUNNING)
                                              .build();
    when(query.get()).thenReturn(pipelineExecution);
    when(workflowExecutionService.getExecutionDetails(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID))
        .thenReturn(aWorkflowExecution().withStatus(SUCCESS).build());
    pipelineService.refreshPipelineExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);

    assertThat(pipelineExecution.getStatus()).isEqualTo(SUCCESS);
    assertThat(pipelineExecution.getPipelineStageExecutions().size()).isEqualTo(3);
    assertThat(pipelineExecution.getPipelineStageExecutions()
                   .stream()
                   .map(PipelineStageExecution::getStatus)
                   .collect(Collectors.toList()))
        .hasSameElementsAs(asList(SUCCESS, SUCCESS, SUCCESS));
    assertThat(pipelineExecution.getPipelineStageExecutions()
                   .stream()
                   .flatMap(pse -> pse.getWorkflowExecutions().stream())
                   .map(WorkflowExecution::getStatus)
                   .collect(Collectors.toList()))
        .hasSize(2)
        .isEqualTo(asList(SUCCESS, SUCCESS));
  }

  private StateMachine createPipelineStateMachine() {
    /*
    pipeline: |DEV--->APPROVAL-->PROD|
     */
    StateMachine sm = new StateMachine();
    sm.setAppId(APP_ID);
    sm.setUuid(WingsTestConstants.STATE_MACHINE_ID);

    EnvState devEnvState = new EnvState("DEV");
    devEnvState.setEnvId("DEV_ENV_ID");
    devEnvState.setWorkflowId(WORKFLOW_ID);
    sm.addState(devEnvState);

    ApprovalState approvalState = new ApprovalState("APPROVAL");
    sm.addState(approvalState);

    EnvState prodEnvState = new EnvState("PROD");
    devEnvState.setEnvId("PROD_ENV_ID");
    devEnvState.setWorkflowId(WORKFLOW_ID);
    sm.addState(prodEnvState);

    sm.setInitialStateName(devEnvState.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(devEnvState)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(approvalState)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(approvalState)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(prodEnvState)
                         .build());

    assertThat(sm.validate()).isTrue();
    return sm;
  }
}
