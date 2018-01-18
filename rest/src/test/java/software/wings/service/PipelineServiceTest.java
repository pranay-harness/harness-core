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
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.EnvState;
import software.wings.utils.JsonUtils;
import software.wings.utils.WingsTestConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 11/3/16.
 */
public class PipelineServiceTest extends WingsBaseTest {
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

  String PIPELINE = "{\n"
      + "    \"_id\" : \"zEH9zdHRRS2GrfkkybqQ5w\",\n"
      + "    \"name\" : \"[DRY RUN] Deploy All SHN Components\",\n"
      + "    \"pipelineStages\" : [ \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shnlogcollectorapp\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"DWsddTiASpWGq3GMb8Cmyg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"s5fWaE2jTACctQOcOZIFuQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"ec-monitor-server\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"cJnSr7wyQjmVkhGo8xSvPQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"ASPmD63cSgGlDdrkbXTmqg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"ec-datacopier-server\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"gbuC0rpYSiuSBhnWAiUzUQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"ufN66JwoSiGniNNKQ8rx6g\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"overlord\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"rJGT-gsIRFikAkA5JSULQw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"ZrcaLodsR5S9l54xltXOPg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"neo-auth-service\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"pDwCaIdET6GPOG-FOLBdyA\",\n"
      + "                        \"ServiceInfra_SSH\" : \"YuV38QpoQl-ceCrXSF1Qlw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"neo-api-gateway\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"lk4MQ3YaTsC8BcV3GOfzcw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"RCD0yO4cTQapoWFZQrqh0w\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"config-audit-api\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"tE4kpJU3SKKQtDd3-KlnqQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"a2hXOSLeR3W_y8EzROHOIw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"config-syncer\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"aNi_Eil7S7m8henv5h3CBQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"-jhMxUw7RAu1RRT3WwuYHg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"watchtower-server\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"2cM_KPpJSrSDAf0BQnPEKQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"iqPPXtwYR2-3MiFe_VWpEg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"events-analytics-service\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"zU-gtPqUQdmLI1cvz0t-5A\",\n"
      + "                        \"ServiceInfra_SSH\" : \"0w61RPXbRvOBN1V7xm8ojA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"caching-service\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"VmvT5ApARyOj8XkUGJcKmg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"xC_SC0shQaWY9XQRsxPdlA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"reporting-service\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"RBWmvtbUTxKEgy1kyqqcsw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"BeAzcecfTVCa4fLtzHLAzQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"service-workflow\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"t-IoOImLQNqvNrGD-9ZFHQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"K5N6GQGtRuSNhCRZqtXbLw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"messenger-server\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"jKIMNnMHQbKyh8FfwTpRxw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"NrBT8NOBS1WuvUgVsBZnrg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shndirectory-service\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"aNt_SxqxTU6U0PmFV55DHg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"2EV-TRU1Qa2Cv5kt_0YxKg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"hadoop-batch-scheduler\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"omlvJxYTSU2hmjnTd2CLnw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"xMNOdLIrTjqBOjBEfOCcbA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shndlpapi-server\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"1qQ0d9cIRV2JNFDHn1piNg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"eLxA23hnSgiZ_mOB-UC0mQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shndirectory-ingestion\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"Js_D4IPJSmqvTIRx8SKycg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"VOgjkEDsSay1C3Jw9ijz3Q\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shnrealtimedlp\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"5wWEuhvdRhCKcNZkHAZaig\",\n"
      + "                        \"ServiceInfra_SSH\" : \"Nnils_LtRzSt9Mv_a4KvhA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shn-sharepoint-dlp\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"JJeIGyExQXq_41_fy3WA2A\",\n"
      + "                        \"ServiceInfra_SSH\" : \"LwJqKSPpTPuCehlFVJVNUg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"hadoop-batch-scripts\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"pe4raDXgRt--7WkEU7rnxg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"6_h4uW5FTc6-ec4-OTv4jg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shn-keyviewfilter\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"Cvb8xkguRiGbjhCs3GOOzw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"RIVTVhMsRFyO5bZRQo-Vhw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shneventsummary-processor\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"A4ED8tmxTyuNczeiroiRrQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"YKT93JMlR9KnqhOAguVsDg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"zeus-server\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"1KvIE-yrSCqQN0Nb4CSX2g\",\n"
      + "                        \"ServiceInfra_SSH\" : \"Dp9QgvpcTLKok35oPIwDZQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shnwebhooks\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"NQmr6UcFROG1RP5_okrssg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"ZLeONfv3QlOrSoU1TVzzsA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"hadoop-batch-distribution\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"XjsnCumIQ22h1hqpsvaSuQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"5dUIZitESgGFwR5iOjFYfw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shnmapreduce\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"IhQr4SO9SP2wo8V40q5Ppw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"lrymbMQcTP6QWDSn8skb9g\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"hadoop-logimporter\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"ZhvAQxiVS1eupnMy0YJoKQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"atpWD9wvQE-QWdIUks1aig\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-wf-coord\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"GIToW9esQnyX3fMTneKDxw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"Jw-8WtjsS9ebb6FPpI50Bg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-data-bridge\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"1GpackQ6SGmRktpd30QswQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"esphBbadS3KxhUsGMNoqHA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-kafka-event-consumer\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"UmnggB6SS66HEuzgBNTA4A\",\n"
      + "                        \"ServiceInfra_SSH\" : \"tlF4tmO7RmmSad49V8yHKA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-user-detection\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"1-PLFawBSv2Ki1ZlNyd7pw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"xGS1mF7fSOKlvzIXID6Tuw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-anomaly\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"_k82Vwt4T0eVeuUDYy9Q3Q\",\n"
      + "                        \"ServiceInfra_SSH\" : \"nTHhW2rWS8u-5RRZrKDkww\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-unmatched-event-summary\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"qSCJOrNdTcqBIib7BRjOlA\",\n"
      + "                        \"ServiceInfra_SSH\" : \"KnuZpEO6QP2Tw_pSz4bNgg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-event-summary\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"gsX443z3Tn--vjGE_dSxQg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"Uwxk8ss2Q-yqP4aLhzl89w\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-event-drilldown\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"FNgK2gcvSGG7xFd8n-8dxw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"jMjbNvy3TvqsktFzguXPHA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-aggregation\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"wHE7yH_GSyyntuZwHnhuZg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"lAUt00i8TDeahlcThg8OFw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dp-enterprise-riskscore\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"dQs4_-MHRSKbUFmfJarS1w\",\n"
      + "                        \"ServiceInfra_SSH\" : \"hfsO3frXT-SpVzFLROLx5A\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"si-batch\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"7iqjucVARWqLixYsFcFXCw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"aeD4XPa6T8mm_uWoL3Trkg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"si-automation-utils\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"LEiExVs7QzSsLn6sOV2MWA\",\n"
      + "                        \"ServiceInfra_SSH\" : \"uVKz1QWnTNWyPsX1qoQsXQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shnanalytics-model-training\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"9HLKCDRcQlKRogEgO1FcUQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"f1rtV-qmRYybmC8xcCHTCQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"malicious-activity-analysis\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"kR7T55x_R7OpHBUISJ0rHw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"mqr2eyhfSbqQGrHmQK32FQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"hadoop-ipclassifier\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"YfAlBCAGTXeBzBhzKHmICQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"K_lEWK50QXeP12C3DKCYMw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"raw-event-drilldown\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"pdzdS7-oQVuZiOslhb8SMQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"_YsERQV6RfmXstWgS89gaA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shnanalytics-events-microservice\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"flWnY3pPQLKrG17tHg4BWQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"uEVYDGeeRXydFe9-ql8swg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"stats-server\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"6g043B_jRoiTKF-UR5tb6w\",\n"
      + "                        \"ServiceInfra_SSH\" : \"iz49IXDYT3KCGFXsOInD6Q\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"si-ml-metadata\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"MlqHZ9oZQg-hjBFHcbDdXA\",\n"
      + "                        \"ServiceInfra_SSH\" : \"8Y1sJh70TgmML_ctOUM7XQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"si-serv\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"-rn3N9ZRRzeu5CR5Sbybbw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"wrV1HyByT5O_z45PEJfxcw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"merlin\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"M0kow_JjTY2Rgqd7waJxYg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"ZVBQuWe6QkKh8mS6AUji1A\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"content-scanner\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"oi3rFniMRtidnVJ6s8B4vQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"UTr-a554SnqIv1agR68J-A\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"streaming_analytics\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"-GsgaSOGROuHvs0KjMKkBw\",\n"
      + "                        \"ServiceInfra_SSH\" : \"DIs9OEVZS1-s4tBd5HIMcw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shnproxyapi\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"P7XUAOkwQfKPU_mgiQ0Khg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"lMXZIfUCSeCqRGL8POjcWA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shnproxy\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"ozuzckbeQFWWflSDZpDUDQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"9luEsduCRDWspTADXVbNvQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"shneventprocessor\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"zmUxhemoTn2p6dNeVwygow\",\n"
      + "                        \"ServiceInfra_SSH\" : \"Gz1e8VZ9Skiqg6DFOHFoEA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"pivot\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"fsum0HU8R4ObLMwH-XXpVg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"vHBsUTp6QF2v8mzQYilOGQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : false,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"dashboard\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"B1U3sNtrSP-rGQ60sF0HfQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"kDRpPgURTVmGhTu6Qn13dw\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"neo-app\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"fvCFKvmuSPqrwNiiPn_4zg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"NeK7t-jwQ5uaycADJbq7JQ\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"policy-management\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"CAidC-vXSe2OvJMPXllZVg\",\n"
      + "                        \"ServiceInfra_SSH\" : \"m_TC60_uRU-3TUY1CV_l9g\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"setup-and-configuration\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"6mQR9Rq1TwqexOXvnzdzUQ\",\n"
      + "                        \"ServiceInfra_SSH\" : \"YHLqAOeUQxqDNzkofc__zg\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }, \n"
      + "        {\n"
      + "            \"parallel\" : true,\n"
      + "            \"pipelineStageElements\" : [ \n"
      + "                {\n"
      + "                    \"name\" : \"usage-analytics\",\n"
      + "                    \"type\" : \"ENV_STATE\",\n"
      + "                    \"properties\" : {\n"
      + "                        \"envId\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"workflowId\" : \"7-fkKHxHS7SsDeWbRa2zCw\"\n"
      + "                    },\n"
      + "                    \"workflowVariables\" : {\n"
      + "                        \"Environment\" : \"RIDY9CCyScmx1wLkz3j1Dw\",\n"
      + "                        \"Service\" : \"A7hs7r_wRhSM2u6u_Xc6dA\",\n"
      + "                        \"ServiceInfra_SSH\" : \"q5m9pEkwRRWXz1IJHZmxLA\"\n"
      + "                    },\n"
      + "                    \"valid\" : true\n"
      + "                }\n"
      + "            ],\n"
      + "            \"valid\" : true\n"
      + "        }\n"
      + "    ],\n"
      + "    \"appId\" : \"BB1xpV5rSmGHersn1KwCnA\",\n"
      + "    \"createdBy\" : {\n"
      + "        \"uuid\" : \"ZQe7_SyeSDmI90r42rLVsA\",\n"
      + "        \"name\" : \"User Name\",\n"
      + "        \"email\" : \"abc@sdef.com\"\n"
      + "    },\n"
      + "    \"createdAt\" : 1516230090707,\n"
      + "    \"lastUpdatedBy\" : {\n"
      + "        \"uuid\" : \"ZQe7_SyeSDmI90r42rLVsA\",\n"
      + "        \"name\" : \"User Name\",\n"
      + "        \"email\" : \"abc@def.com\"\n"
      + "    },\n"
      + "    \"lastUpdatedAt\" : 1516231870887\n"
      + "}";
}
