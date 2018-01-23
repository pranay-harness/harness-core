package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.FailureStrategy.FailureStrategyBuilder.aFailureStrategy;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.resource.Loader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RepairActionCode;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 11/3/16.
 */
public class PipelineServiceTest extends WingsBaseTest {
  private static String PIPELINE = Loader.load("pipeline/dry_run.json");

  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private TriggerService triggerService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private WorkflowService workflowService;

  @Mock private Query<PipelineExecution> query;
  @Mock private FieldEnd end;

  @Mock private JobScheduler jobScheduler;

  @Inject @InjectMocks private PipelineService pipelineService;

  @Captor private ArgumentCaptor<Pipeline> pipelineArgumentCaptor;
  @Captor private ArgumentCaptor<StateMachine> stateMachineArgumentCaptor;

  public PipelineServiceTest() throws IOException {}

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

  @Test
  public void shouldCreatePipelineFromJson() {
    String appId = "BB1xpV5rSmGHersn1KwCnA";
    String workflowId = "7-fkKHxHS7SsDeWbRa2zCw";

    Pipeline pipeline = JsonUtils.asObject(PIPELINE, Pipeline.class);
    when(workflowService.readWorkflow(appId, workflowId))
        .thenReturn(aWorkflow().withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build());

    when(wingsPersistence.saveAndGet(eq(Pipeline.class), eq(pipeline))).thenReturn(pipeline);
    when(workflowService.stencilMap()).thenReturn(ImmutableMap.of("ENV_STATE", StateType.ENV_STATE));

    pipelineService.createPipeline(pipeline);

    verify(wingsPersistence).saveAndGet(eq(Pipeline.class), pipelineArgumentCaptor.capture());
    assertThat(pipelineArgumentCaptor.getValue()).isNotNull();

    verify(wingsPersistence).saveAndGet(eq(StateMachine.class), stateMachineArgumentCaptor.capture());
    StateMachine stateMachine = stateMachineArgumentCaptor.getValue();
    assertThat(stateMachine).isNotNull();
    assertThat(stateMachine.getStates()).isNotNull();
    assertThat(stateMachine.getStates().size()).isGreaterThan(60);
  }
  @Test
  public void shouldCreateLargePipeline() {
    List<String> workflowIds = new ArrayList<>();
    List<PipelineStage> pipelineStages = new ArrayList<>();
    for (int i = 0; i < 60; i++) {
      String uuid = UUIDGenerator.getUuid();
      workflowIds.add(uuid);
      when(workflowService.readWorkflow(APP_ID, uuid))
          .thenReturn(aWorkflow().withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build());

      Map<String, Object> properties = new HashMap<>();
      properties.put("envId", UUIDGenerator.getUuid());
      properties.put("workflowId", uuid);
      PipelineStage pipelineStage =
          new PipelineStage(asList(new PipelineStageElement("SE" + i, ENV_STATE.name(), properties)));
      pipelineStage.setName("STAGE" + i);
      if (i % 16 == 0) {
        pipelineStage.setParallel(false);
      } else {
        pipelineStage.setParallel(true);
      }
      pipelineStages.add(pipelineStage);
    }

    Pipeline pipeline = aPipeline()
                            .withName("pipeline1")
                            .withAppId(APP_ID)
                            .withUuid(PIPELINE_ID)
                            .withPipelineStages(pipelineStages)
                            .build();

    when(wingsPersistence.saveAndGet(eq(Pipeline.class), eq(pipeline))).thenReturn(pipeline);
    when(workflowService.stencilMap()).thenReturn(ImmutableMap.of("ENV_STATE", StateType.ENV_STATE));

    pipelineService.createPipeline(pipeline);

    verify(wingsPersistence).saveAndGet(eq(Pipeline.class), pipelineArgumentCaptor.capture());
    assertThat(pipelineArgumentCaptor.getValue()).isNotNull();

    verify(wingsPersistence).saveAndGet(eq(StateMachine.class), stateMachineArgumentCaptor.capture());
    StateMachine stateMachine = stateMachineArgumentCaptor.getValue();
    assertThat(stateMachine).isNotNull();
    assertThat(stateMachine.getStates().size()).isGreaterThan(60);
  }

  @Test
  public void shouldCreatePipeline() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("envId", ENV_ID);
    properties.put("workflowId", WORKFLOW_ID);
    PipelineStage pipelineStage =
        new PipelineStage(asList(new PipelineStageElement("SE", ENV_STATE.name(), properties)));
    pipelineStage.setName("STAGE1");

    FailureStrategy failureStrategy =
        aFailureStrategy().withRepairActionCode(RepairActionCode.MANUAL_INTERVENTION).build();
    Pipeline pipeline = aPipeline()
                            .withName("pipeline1")
                            .withAppId(APP_ID)
                            .withUuid(PIPELINE_ID)
                            .withPipelineStages(asList(pipelineStage))
                            .withFailureStrategies(asList(failureStrategy))
                            .build();

    when(wingsPersistence.saveAndGet(eq(Pipeline.class), eq(pipeline))).thenReturn(pipeline);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID))
        .thenReturn(aWorkflow().withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build());
    when(workflowService.stencilMap()).thenReturn(ImmutableMap.of("ENV_STATE", StateType.ENV_STATE));

    pipelineService.createPipeline(pipeline);

    verify(wingsPersistence).saveAndGet(eq(Pipeline.class), pipelineArgumentCaptor.capture());
    assertThat(pipelineArgumentCaptor.getValue())
        .isNotNull()
        .extracting("failureStrategies")
        .doesNotContainNull()
        .contains(asList(failureStrategy));
  }

  @Test
  public void shouldGetPipeline() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(aPipeline()
                        .withAppId(APP_ID)
                        .withUuid(PIPELINE_ID)
                        .withPipelineStages(asList(new PipelineStage(asList(new PipelineStageElement(
                            "SE", ENV_STATE.name(), ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))))))
                        .build());

    Pipeline pipeline = pipelineService.readPipeline(APP_ID, PIPELINE_ID, false);
    assertThat(pipeline).isNotNull().hasFieldOrPropertyWithValue("uuid", PIPELINE_ID);
    verify(wingsPersistence).get(Pipeline.class, APP_ID, PIPELINE_ID);
  }

  @Test
  public void shouldGetPipelineWithServices() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(aPipeline()
                        .withAppId(APP_ID)
                        .withUuid(PIPELINE_ID)
                        .withPipelineStages(asList(new PipelineStage(asList(new PipelineStageElement(
                            "SE", ENV_STATE.name(), ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))))))
                        .build());

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID))
        .thenReturn(aWorkflow().withServices(asList(aService().withUuid(SERVICE_ID).build())).build());

    Pipeline pipeline = pipelineService.readPipeline(APP_ID, PIPELINE_ID, true);
    assertThat(pipeline).isNotNull().hasFieldOrPropertyWithValue("uuid", PIPELINE_ID);
    assertThat(pipeline.getServices()).hasSize(1).extracting("uuid").isEqualTo(asList(SERVICE_ID));
    verify(wingsPersistence).get(Pipeline.class, APP_ID, PIPELINE_ID);
  }

  @Test
  public void shouldGetPipelineWithTemplatizedServices() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(aPipeline()
                        .withAppId(APP_ID)
                        .withUuid(PIPELINE_ID)
                        .withPipelineStages(asList(new PipelineStage(asList(new PipelineStageElement("SE",
                            ENV_STATE.name(), ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID),
                            ImmutableMap.of("Environment", ENV_ID, "Service", SERVICE_ID))))))
                        .build());

    when(serviceResourceService.list(any(), anyBoolean(), anyBoolean()))
        .thenReturn(aPageResponse().withResponse(asList(aService().withUuid(SERVICE_ID).build())).build());
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID))
        .thenReturn(
            aWorkflow()
                .withOrchestrationWorkflow(
                    aCanaryOrchestrationWorkflow()
                        .withUserVariables(asList(
                            aVariable().withEntityType(SERVICE).withName("Service").withValue(SERVICE_ID).build()))
                        .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                        .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                        .build())
                .withServices(asList(aService().withUuid(SERVICE_ID).build()))
                .build());

    Pipeline pipeline = pipelineService.readPipeline(APP_ID, PIPELINE_ID, true);
    assertThat(pipeline).isNotNull().hasFieldOrPropertyWithValue("uuid", PIPELINE_ID);
    assertThat(pipeline.getServices()).hasSize(1).extracting("uuid").isEqualTo(asList(SERVICE_ID));

    verify(wingsPersistence).get(Pipeline.class, APP_ID, PIPELINE_ID);
  }

  @Test
  public void shouldDeletePipeline() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(aPipeline()
                        .withAppId(APP_ID)
                        .withUuid(PIPELINE_ID)
                        .withPipelineStages(asList(new PipelineStage(asList(new PipelineStageElement(
                            "SE", ENV_STATE.name(), ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))))))
                        .build());
    when(wingsPersistence.delete(any(Pipeline.class))).thenReturn(true);

    assertThat(pipelineService.deletePipeline(APP_ID, PIPELINE_ID)).isTrue();
  }

  @Test(expected = WingsException.class)
  public void deletePipelineExecutionInProgress() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(aPipeline()
                        .withAppId(APP_ID)
                        .withUuid(PIPELINE_ID)
                        .withPipelineStages(asList(new PipelineStage(asList(new PipelineStageElement(
                            "SE", ENV_STATE.name(), ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))))))
                        .build());
    PipelineExecution pipelineExecution = aPipelineExecution().withStatus(ExecutionStatus.RUNNING).build();
    PageResponse pageResponse = aPageResponse().withResponse(asList(pipelineExecution)).build();
    when(wingsPersistence.query(PipelineExecution.class,
             aPageRequest().addFilter("appId", EQ, APP_ID).addFilter("pipelineId", EQ, PIPELINE_ID).build()))
        .thenReturn(pageResponse);
    pipelineService.deletePipeline(APP_ID, PIPELINE_ID);
  }

  @Test
  public void shouldPruneDescendingObjects() {
    pipelineService.pruneDescendingEntities(APP_ID, PIPELINE_ID);

    InOrder inOrder = inOrder(wingsPersistence, workflowService, triggerService);
    inOrder.verify(triggerService).pruneByPipeline(APP_ID, PIPELINE_ID);
  }
}
