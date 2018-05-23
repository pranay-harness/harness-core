package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SSH_PASSWORD;
import static software.wings.beans.EntityType.SSH_USER;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import io.harness.resource.Loader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.beans.FailureStrategy;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RepairActionCode;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.dl.HQuery;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PipelineServiceTest extends WingsBaseTest {
  private static final String PIPELINE = Loader.load("pipeline/dry_run.json");

  @Mock private AppService appService;
  @Mock private TriggerService triggerService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private WorkflowService workflowService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private UpdateOperations<Pipeline> updateOperations;
  @Mock private HQuery<PipelineExecution> query;
  @Mock private HQuery<Pipeline> pipelineQuery;
  @Mock private JobScheduler jobScheduler;
  @Mock private YamlDirectoryService yamlDirectoryService;

  @Inject @InjectMocks private PipelineService pipelineService;

  @Captor private ArgumentCaptor<Pipeline> pipelineArgumentCaptor;
  @Captor private ArgumentCaptor<StateMachine> stateMachineArgumentCaptor;

  @Before
  public void setUp() {
    when(wingsPersistence.createQuery(PipelineExecution.class)).thenReturn(query);
    when(wingsPersistence.createQuery(Pipeline.class)).thenReturn(pipelineQuery);
    when(pipelineQuery.filter(any(), any())).thenReturn(pipelineQuery);
    when(wingsPersistence.createUpdateOperations(Pipeline.class)).thenReturn(updateOperations);
    when(appService.get(APP_ID)).thenReturn(anApplication().withUuid(APP_ID).withName(APP_NAME).build());
    when(updateOperations.set(any(), any())).thenReturn(updateOperations);
    when(updateOperations.unset(any())).thenReturn(updateOperations);
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

    pipelineService.save(pipeline);

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
    for (int index = 0; index < 60; index++) {
      String uuid = generateUuid();
      workflowIds.add(uuid);
      when(workflowService.readWorkflow(APP_ID, uuid))
          .thenReturn(aWorkflow().withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build());

      Map<String, Object> properties = new HashMap<>();
      properties.put("envId", generateUuid());
      properties.put("workflowId", uuid);
      PipelineStage pipelineStage = PipelineStage.builder()
                                        .pipelineStageElements(asList(PipelineStageElement.builder()
                                                                          .name("STAGE" + index)
                                                                          .type(ENV_STATE.name())
                                                                          .properties(properties)
                                                                          .build()))
                                        .build();
      if (index % 16 == 0) {
        pipelineStage.setParallel(false);
      } else {
        pipelineStage.setParallel(true);
      }
      pipelineStages.add(pipelineStage);
    }

    Pipeline pipeline =
        Pipeline.builder().name("pipeline1").appId(APP_ID).uuid(PIPELINE_ID).pipelineStages(pipelineStages).build();

    when(wingsPersistence.saveAndGet(eq(Pipeline.class), eq(pipeline))).thenReturn(pipeline);
    when(workflowService.stencilMap()).thenReturn(ImmutableMap.of("ENV_STATE", StateType.ENV_STATE));

    pipelineService.save(pipeline);

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
        PipelineStage.builder()
            .pipelineStageElements(asList(
                PipelineStageElement.builder().name("STAGE1").type(ENV_STATE.name()).properties(properties).build()))
            .build();

    FailureStrategy failureStrategy =
        FailureStrategy.builder().repairActionCode(RepairActionCode.MANUAL_INTERVENTION).build();
    Pipeline pipeline = Pipeline.builder()
                            .name("pipeline1")
                            .appId(APP_ID)
                            .uuid(PIPELINE_ID)
                            .pipelineStages(asList(pipelineStage))
                            .failureStrategies(asList(failureStrategy))
                            .build();

    when(wingsPersistence.saveAndGet(eq(Pipeline.class), eq(pipeline))).thenReturn(pipeline);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID))
        .thenReturn(aWorkflow().withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build());
    when(workflowService.stencilMap()).thenReturn(ImmutableMap.of("ENV_STATE", StateType.ENV_STATE));

    pipelineService.save(pipeline);

    verify(wingsPersistence).saveAndGet(eq(Pipeline.class), pipelineArgumentCaptor.capture());
    Pipeline argumentCaptorValue = pipelineArgumentCaptor.getValue();
    assertThat(argumentCaptorValue)
        .isNotNull()
        .extracting("failureStrategies")
        .doesNotContainNull()
        .contains(asList(failureStrategy));
    assertThat(argumentCaptorValue.getKeywords())
        .isNotNull()
        .contains(WorkflowType.PIPELINE.name().toLowerCase(), pipeline.getName().toLowerCase());
  }

  @Test
  public void shouldUpdatePipeline() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("envId", ENV_ID);
    properties.put("workflowId", WORKFLOW_ID);
    PipelineStage pipelineStage =
        PipelineStage.builder()
            .pipelineStageElements(asList(
                PipelineStageElement.builder().name("STAGE1").type(ENV_STATE.name()).properties(properties).build()))
            .build();

    FailureStrategy failureStrategy =
        FailureStrategy.builder().repairActionCode(RepairActionCode.MANUAL_INTERVENTION).build();
    Pipeline pipeline = Pipeline.builder()
                            .name("pipeline1")
                            .appId(APP_ID)
                            .uuid(PIPELINE_ID)
                            .pipelineStages(asList(pipelineStage))
                            .failureStrategies(asList(failureStrategy))
                            .build();

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID))
        .thenReturn(aWorkflow().withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build());
    when(workflowService.stencilMap()).thenReturn(ImmutableMap.of("ENV_STATE", StateType.ENV_STATE));

    pipeline.setName("Changed Pipeline");
    pipeline.setDescription("Description changed");

    when(wingsPersistence.get(Pipeline.class, pipeline.getAppId(), pipeline.getUuid())).thenReturn(pipeline);

    Pipeline updatedPipeline = pipelineService.update(pipeline);

    assertThat(updatedPipeline)
        .isNotNull()
        .extracting("failureStrategies")
        .doesNotContainNull()
        .contains(asList(failureStrategy));

    verify(wingsPersistence).get(Pipeline.class, pipeline.getAppId(), pipeline.getUuid());
    verify(wingsPersistence).createQuery(Pipeline.class);
    verify(pipelineQuery, times(2)).filter(any(), any());
    verify(wingsPersistence).createUpdateOperations(Pipeline.class);
  }

  @Test
  public void shouldGetPipeline() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(Pipeline.builder()
                        .appId(APP_ID)
                        .uuid(PIPELINE_ID)
                        .pipelineStages(
                            asList(PipelineStage.builder()
                                       .pipelineStageElements(asList(
                                           PipelineStageElement.builder()
                                               .name("SE")
                                               .type(ENV_STATE.name())
                                               .properties(ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))
                                               .build()))
                                       .build()))
                        .build());

    Pipeline pipeline = pipelineService.readPipeline(APP_ID, PIPELINE_ID, false);
    assertThat(pipeline).isNotNull().hasFieldOrPropertyWithValue("uuid", PIPELINE_ID);
    verify(wingsPersistence).get(Pipeline.class, APP_ID, PIPELINE_ID);
  }

  @Test
  public void shouldGetPipelineWithServices() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(Pipeline.builder()
                        .appId(APP_ID)
                        .uuid(PIPELINE_ID)
                        .pipelineStages(
                            asList(PipelineStage.builder()
                                       .pipelineStageElements(asList(
                                           PipelineStageElement.builder()
                                               .name("SE")
                                               .type(ENV_STATE.name())
                                               .properties(ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))
                                               .build()))
                                       .build()))
                        .build());

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID))
        .thenReturn(
            aWorkflow()
                .withServices(asList(Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build()))
                .build());

    Pipeline pipeline = pipelineService.readPipeline(APP_ID, PIPELINE_ID, true);
    assertThat(pipeline).isNotNull().hasFieldOrPropertyWithValue("uuid", PIPELINE_ID);
    assertThat(pipeline.getServices()).hasSize(1).extracting("uuid").isEqualTo(asList(SERVICE_ID));
    verify(wingsPersistence).get(Pipeline.class, APP_ID, PIPELINE_ID);
  }

  @Test
  public void shouldGetPipelineWithTemplatizedServices() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(
            Pipeline.builder()
                .appId(APP_ID)
                .uuid(PIPELINE_ID)
                .pipelineStages(
                    asList(PipelineStage.builder()
                               .pipelineStageElements(asList(
                                   PipelineStageElement.builder()
                                       .name("SE")
                                       .type(ENV_STATE.name())
                                       .properties(ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))
                                       .workflowVariables(ImmutableMap.of("Environment", ENV_ID, "Service", SERVICE_ID))
                                       .build()))
                               .build()))
                .build());

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
                .withServices(asList(Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build()))
                .build());

    when(workflowService.getResolvedServices(any(Workflow.class), any()))
        .thenReturn(asList(Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build()));
    Pipeline pipeline = pipelineService.readPipeline(APP_ID, PIPELINE_ID, true);
    assertThat(pipeline).isNotNull().hasFieldOrPropertyWithValue("uuid", PIPELINE_ID);
    assertThat(pipeline.getServices()).hasSize(1).extracting("uuid").isEqualTo(asList(SERVICE_ID));

    verify(wingsPersistence).get(Pipeline.class, APP_ID, PIPELINE_ID);
  }

  @Test
  public void shouldDeletePipeline() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(Pipeline.builder()
                        .appId(APP_ID)
                        .uuid(PIPELINE_ID)
                        .pipelineStages(
                            asList(PipelineStage.builder()
                                       .pipelineStageElements(asList(
                                           PipelineStageElement.builder()
                                               .name("SE")
                                               .type(ENV_STATE.name())
                                               .properties(ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))
                                               .build()))
                                       .build()))
                        .build());
    when(wingsPersistence.delete(Pipeline.class, APP_ID, PIPELINE_ID)).thenReturn(true);

    assertThat(pipelineService.deletePipeline(APP_ID, PIPELINE_ID)).isTrue();
  }

  @Test(expected = WingsException.class)
  public void deletePipelineExecutionInProgress() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(Pipeline.builder()
                        .appId(APP_ID)
                        .uuid(PIPELINE_ID)
                        .pipelineStages(
                            asList(PipelineStage.builder()
                                       .pipelineStageElements(asList(
                                           PipelineStageElement.builder()
                                               .name("SE")
                                               .type(ENV_STATE.name())
                                               .properties(ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))
                                               .build()))
                                       .build()))
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

  @Test
  public void shouldListPipelines() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("envId", ENV_ID);
    properties.put("workflowId", WORKFLOW_ID);
    PipelineStage pipelineStage =
        PipelineStage.builder()
            .pipelineStageElements(asList(
                PipelineStageElement.builder().name("STAGE1").type(ENV_STATE.name()).properties(properties).build()))
            .build();

    FailureStrategy failureStrategy =
        FailureStrategy.builder().repairActionCode(RepairActionCode.MANUAL_INTERVENTION).build();
    Pipeline pipeline = Pipeline.builder()
                            .name("pipeline1")
                            .appId(APP_ID)
                            .uuid(PIPELINE_ID)
                            .pipelineStages(asList(pipelineStage))
                            .failureStrategies(asList(failureStrategy))
                            .build();

    Map<String, Object> properties2 = new HashMap<>();
    properties2.put("envId", ENV_ID);
    properties2.put("workflowId", WORKFLOW_ID);
    PipelineStage pipelineStage2 =
        PipelineStage.builder()
            .pipelineStageElements(asList(PipelineStageElement.builder()
                                              .workflowVariables(ImmutableMap.of("Environment", ENV_ID, "Serivice",
                                                  SERVICE_ID, "ServiceInfra", INFRA_MAPPING_ID))
                                              .name("STAGE1")
                                              .type(ENV_STATE.name())
                                              .properties(properties)
                                              .build()))
            .build();

    Pipeline pipeline2 = Pipeline.builder()
                             .name("pipeline2")
                             .appId(APP_ID)
                             .uuid(PIPELINE_ID)
                             .pipelineStages(asList(pipelineStage2))
                             .build();
    when(wingsPersistence.query(Pipeline.class, aPageRequest().build()))
        .thenReturn(aPageResponse().withResponse(asList(pipeline, pipeline2)).build());

    PageResponse pageResponse = pipelineService.listPipelines(aPageRequest().build());
    List<Pipeline> pipelines = pageResponse.getResponse();
    assertThat(pipelines).isNotEmpty().size().isEqualTo(2);
    assertThat(pipelines.get(0).getName()).isEqualTo("pipeline1");
    assertThat(pipelines.get(0).getAppId()).isEqualTo(APP_ID);
    assertThat(pipelines.get(1).getName()).isEqualTo("pipeline2");
    assertThat(pipelines.get(1).getAppId()).isEqualTo(APP_ID);
  }

  @Test
  public void shouldListPipelinesWithDetails() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("envId", ENV_ID);
    properties.put("workflowId", WORKFLOW_ID);
    PipelineStage pipelineStage =
        PipelineStage.builder()
            .pipelineStageElements(asList(
                PipelineStageElement.builder().name("STAGE1").type(ENV_STATE.name()).properties(properties).build()))
            .build();

    FailureStrategy failureStrategy =
        FailureStrategy.builder().repairActionCode(RepairActionCode.MANUAL_INTERVENTION).build();
    Pipeline pipeline = Pipeline.builder()
                            .name("pipeline1")
                            .appId(APP_ID)
                            .uuid(PIPELINE_ID)
                            .pipelineStages(asList(pipelineStage))
                            .failureStrategies(asList(failureStrategy))
                            .build();

    Map<String, Object> properties2 = new HashMap<>();
    properties2.put("envId", ENV_ID);
    properties2.put("workflowId", WORKFLOW_ID);
    PipelineStage pipelineStage2 =
        PipelineStage.builder()
            .pipelineStageElements(asList(PipelineStageElement.builder()
                                              .workflowVariables(ImmutableMap.of("Environment", ENV_ID, "Service",
                                                  SERVICE_ID, "ServiceInfra", INFRA_MAPPING_ID))
                                              .name("STAGE1")
                                              .type(ENV_STATE.name())
                                              .properties(properties)
                                              .build()))
            .build();

    Pipeline pipeline2 = Pipeline.builder()
                             .name("pipeline2")
                             .appId(APP_ID)
                             .uuid(PIPELINE_ID)
                             .pipelineStages(asList(pipelineStage2))
                             .build();
    when(wingsPersistence.query(Pipeline.class, aPageRequest().build()))
        .thenReturn(aPageResponse().withResponse(asList(pipeline, pipeline2)).build());

    Workflow workflow = aWorkflow().withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build();
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    PageRequest<WorkflowExecution> workflowExecutionPageRequest = aPageRequest()
                                                                      .withLimit("2")
                                                                      .addFilter("workflowId", EQ, pipeline.getUuid())
                                                                      .addFilter("appId", EQ, pipeline.getAppId())
                                                                      .build();
    when(workflowExecutionService.listExecutions(workflowExecutionPageRequest, false, false, false, false))
        .thenReturn(aPageResponse().build());

    PageResponse pageResponse = pipelineService.listPipelines(aPageRequest().build(), true, 2);
    List<Pipeline> pipelines = pageResponse.getResponse();
    assertThat(pipelines).isNotEmpty().size().isEqualTo(2);
    assertThat(pipelines.get(0).getName()).isEqualTo("pipeline1");
    assertThat(pipelines.get(0).getAppId()).isEqualTo(APP_ID);
    assertThat(pipelines.get(0).isValid()).isEqualTo(true);
    assertThat(pipelines.get(1).getName()).isEqualTo("pipeline2");
    assertThat(pipelines.get(1).getAppId()).isEqualTo(APP_ID);
    assertThat(pipelines.get(1).isValid()).isEqualTo(false);
    assertThat(pipelines.get(1).getValidationMessage()).isNotEmpty();
    assertThat(pipelines.get(1).isHasSshInfraMapping()).isEqualTo(false);

    verify(workflowService)
        .getResolvedInfraMappings(
            workflow, ImmutableMap.of("Environment", ENV_ID, "Service", SERVICE_ID, "ServiceInfra", INFRA_MAPPING_ID));
    verify(workflowExecutionService, times(2)).listExecutions(workflowExecutionPageRequest, false, false, false, false);
  }

  @Test
  public void shouldListPipelinesWithDetailsWithSshInfraMapping() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("envId", ENV_ID);
    properties.put("workflowId", WORKFLOW_ID);
    PipelineStage pipelineStage =
        PipelineStage.builder()
            .pipelineStageElements(asList(
                PipelineStageElement.builder().name("STAGE1").type(ENV_STATE.name()).properties(properties).build()))
            .build();

    FailureStrategy failureStrategy =
        FailureStrategy.builder().repairActionCode(RepairActionCode.MANUAL_INTERVENTION).build();
    Pipeline pipeline = Pipeline.builder()
                            .name("pipeline1")
                            .appId(APP_ID)
                            .uuid(PIPELINE_ID)
                            .pipelineStages(asList(pipelineStage))
                            .failureStrategies(asList(failureStrategy))
                            .build();

    when(wingsPersistence.query(Pipeline.class, aPageRequest().build()))
        .thenReturn(aPageResponse().withResponse(asList(pipeline)).build());

    Workflow workflow = aWorkflow().withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build();
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.getResolvedInfraMappings(workflow, null))
        .thenReturn(asList(anAwsInfrastructureMapping()
                               .withInfraMappingType(InfrastructureMappingType.AWS_SSH.name())
                               .withDeploymentType(DeploymentType.SSH.name())
                               .build()));

    PageRequest<WorkflowExecution> workflowExecutionPageRequest = aPageRequest()
                                                                      .withLimit("2")
                                                                      .addFilter("workflowId", EQ, pipeline.getUuid())
                                                                      .addFilter("appId", EQ, pipeline.getAppId())
                                                                      .build();
    when(workflowExecutionService.listExecutions(workflowExecutionPageRequest, false, false, false, false))
        .thenReturn(aPageResponse().build());

    PageResponse pageResponse = pipelineService.listPipelines(aPageRequest().build(), true, 2);
    List<Pipeline> pipelines = pageResponse.getResponse();
    assertThat(pipelines).isNotEmpty().size().isEqualTo(1);
    assertThat(pipelines.get(0).getName()).isEqualTo("pipeline1");
    assertThat(pipelines.get(0).getAppId()).isEqualTo(APP_ID);
    assertThat(pipelines.get(0).isValid()).isEqualTo(true);
    assertThat(pipelines.get(0).isHasSshInfraMapping()).isEqualTo(true);

    verify(workflowService).getResolvedInfraMappings(workflow, null);
    verify(workflowExecutionService, times(1)).listExecutions(workflowExecutionPageRequest, false, false, false, false);
  }

  @Test
  public void shouldGetRequiredEntities() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(Pipeline.builder()
                        .appId(APP_ID)
                        .uuid(PIPELINE_ID)
                        .pipelineStages(
                            asList(PipelineStage.builder()
                                       .pipelineStageElements(asList(
                                           PipelineStageElement.builder()
                                               .name("SE")
                                               .type(ENV_STATE.name())
                                               .properties(ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))
                                               .build()))
                                       .build()))
                        .build());

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID))
        .thenReturn(
            aWorkflow()
                .withOrchestrationWorkflow(
                    aCanaryOrchestrationWorkflow()
                        .withUserVariables(asList(
                            aVariable().withEntityType(SERVICE).withName("Service").withValue(SERVICE_ID).build()))
                        .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                        .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                        .withRequiredEntityTypes(ImmutableSet.of(ARTIFACT, SSH_USER, SSH_PASSWORD))
                        .build())
                .withServices(asList(Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build()))
                .build());
    List<EntityType> requiredEntities = pipelineService.getRequiredEntities(APP_ID, PIPELINE_ID);
    assertThat(requiredEntities).isNotEmpty().contains(ARTIFACT, SSH_USER, SSH_PASSWORD);

    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
    verify(wingsPersistence).get(Pipeline.class, APP_ID, PIPELINE_ID);
  }

  @Test
  public void shouldGetRequiredEntitiesForBuildPipeline() {
    when(wingsPersistence.get(Pipeline.class, APP_ID, PIPELINE_ID))
        .thenReturn(Pipeline.builder()
                        .appId(APP_ID)
                        .uuid(PIPELINE_ID)
                        .pipelineStages(
                            asList(PipelineStage.builder()
                                       .pipelineStageElements(asList(
                                           PipelineStageElement.builder()
                                               .name("SE")
                                               .type(ENV_STATE.name())
                                               .properties(ImmutableMap.of("envId", ENV_ID, "workflowId", WORKFLOW_ID))
                                               .build()))
                                       .build()))
                        .build());

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID))
        .thenReturn(aWorkflow()
                        .withOrchestrationWorkflow(
                            aBuildOrchestrationWorkflow()
                                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                                .build())
                        .build());
    List<EntityType> requiredEntities = pipelineService.getRequiredEntities(APP_ID, PIPELINE_ID);
    assertThat(requiredEntities).isEmpty();

    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
    verify(wingsPersistence).get(Pipeline.class, APP_ID, PIPELINE_ID);
  }
}
