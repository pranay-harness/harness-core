package io.harness.cdng.pipeline.executions.service;

import static io.harness.pms.contracts.plan.TriggerType.MANUAL;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.utils.PageTestUtils.getPage;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.executions.PipelineExecutionHelper;
import io.harness.engine.OrchestrationService;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.interrupts.Interrupt;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionInterruptType;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummaryFilter;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.plan.Plan;
import io.harness.pms.contracts.advisers.InterruptConfig;
import io.harness.pms.contracts.advisers.IssuedBy;
import io.harness.pms.contracts.advisers.ManualIssuer;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.repositories.pipeline.PipelineExecutionRepository;
import io.harness.rule.Owner;
import io.harness.steps.StepOutcomeGroup;

import io.fabric8.utils.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.CDC)
@RunWith(PowerMockRunner.class)
public class NgPipelineExecutionServiceImplTest extends CategoryTest {
  public static final String ACCOUNT_ID = "accountId";
  public static final String ORG_ID = "orgId";
  public static final String PROJECT_ID = "projectId";
  public static final String PLAN_EXECUTION_ID = "projectId";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private OrchestrationService orchestrationService;
  @Mock private PipelineExecutionRepository pipelineExecutionRepository;
  @Mock private NGPipelineService ngPipelineService;
  @Mock private PipelineExecutionHelper pipelineExecutionHelper;
  @InjectMocks
  private final NgPipelineExecutionServiceImpl ngPipelineExecutionService = spy(new NgPipelineExecutionServiceImpl());

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testUpdateStatusForGivenStageNode() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .node(PlanNodeProto.newBuilder().setUuid("planNodeId").setGroup(StepOutcomeGroup.STAGE.name()).build())
            .build();
    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder().build();
    doReturn(pipelineExecutionSummary)
        .when(ngPipelineExecutionService)
        .getByPlanExecutionId(ACCOUNT_ID, ORG_ID, PROJECT_ID, PLAN_EXECUTION_ID);

    PipelineExecutionHelper.StageIndex stageIndex = PipelineExecutionHelper.StageIndex.builder().build();
    doReturn(stageIndex).when(pipelineExecutionHelper).findStageIndexByPlanNodeId(emptyList(), "planNodeId");
    Update update = new Update();
    doReturn(update).when(pipelineExecutionHelper).getCDStageExecutionSummaryStatusUpdate(stageIndex, nodeExecution);

    ngPipelineExecutionService.updateStatusForGivenNode(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, PLAN_EXECUTION_ID, nodeExecution);

    verify(pipelineExecutionHelper).getCDStageExecutionSummaryStatusUpdate(stageIndex, nodeExecution);
    verify(pipelineExecutionRepository).findAndUpdate(PLAN_EXECUTION_ID, update);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testUpdateStatusForPipelineNode() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .node(PlanNodeProto.newBuilder().setGroup(StepOutcomeGroup.PIPELINE.name()).build())
            .build();
    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder().build();
    doReturn(pipelineExecutionSummary)
        .when(ngPipelineExecutionService)
        .getByPlanExecutionId(ACCOUNT_ID, ORG_ID, PROJECT_ID, PLAN_EXECUTION_ID);

    ngPipelineExecutionService.updateStatusForGivenNode(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, PLAN_EXECUTION_ID, nodeExecution);

    verify(pipelineExecutionHelper).updatePipelineExecutionStatus(pipelineExecutionSummary, nodeExecution);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetByPlanExecutionId() {
    when(pipelineExecutionRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndPlanExecutionId(
             ACCOUNT_ID, ORG_ID, PROJECT_ID, PLAN_EXECUTION_ID))
        .thenReturn(Optional.of(PipelineExecutionSummary.builder().build()));

    ngPipelineExecutionService.getByPlanExecutionId(ACCOUNT_ID, ORG_ID, PROJECT_ID, PLAN_EXECUTION_ID);

    verify(pipelineExecutionRepository)
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndPlanExecutionId(
            ACCOUNT_ID, ORG_ID, PROJECT_ID, PLAN_EXECUTION_ID);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRegisterInterrupt() {
    InterruptPackage interruptPackage =
        InterruptPackage.builder()
            .interruptType(PipelineExecutionInterruptType.ABORT.getExecutionInterruptType())
            .planExecutionId(PLAN_EXECUTION_ID)
            .interruptConfig(
                InterruptConfig.newBuilder()
                    .setIssuedBy(IssuedBy.newBuilder().setManualIssuer(ManualIssuer.newBuilder().build()).build())
                    .build())
            .build();
    when(orchestrationService.registerInterrupt(interruptPackage))
        .thenReturn(
            Interrupt.builder().uuid("uuid").type(InterruptType.ABORT_ALL).planExecutionId(PLAN_EXECUTION_ID).build());

    ngPipelineExecutionService.registerInterrupt(PipelineExecutionInterruptType.ABORT, PLAN_EXECUTION_ID);

    verify(orchestrationService).registerInterrupt(interruptPackage);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetExecutions() {
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(pipelineExecutionRepository.findAll(any(Criteria.class), any(Pageable.class)))
        .thenReturn(getPage(emptyList(), 0));
    when(ngPipelineService.getPipelineIdentifierToName(any(), anyString(), anyString(), any()))
        .thenReturn(new HashMap<>());
    PipelineExecutionSummaryFilter pipelineExecutionSummaryFilter =
        PipelineExecutionSummaryFilter.builder()
            .executionStatuses(Lists.newArrayList(ExecutionStatus.EXPIRED))
            .endTime(18L)
            .searchTerm("searchTerm")
            .envIdentifiers(Lists.newArrayList("envId"))
            .serviceIdentifiers(Lists.newArrayList("serviceId"))
            .environmentTypes(EnvironmentType.Production)
            .pipelineIdentifiers(singletonList("pipelineId"))
            .startTime(0L)
            .build();

    Page<PipelineExecutionSummary> pipelineExecutionSummaries = ngPipelineExecutionService.getExecutions(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, Pageable.unpaged(), pipelineExecutionSummaryFilter);

    verify(pipelineExecutionRepository).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));
    verify(ngPipelineService).getPipelineIdentifierToName(any(), anyString(), anyString(), any());

    Criteria criteria = criteriaArgumentCaptor.getValue();
    Document criteriaObject = criteria.getCriteriaObject();

    assertEquals(11, criteriaObject.size());
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.executionStatus));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.environmentTypes));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.startedAt));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.endedAt));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.envIdentifiers));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.serviceIdentifiers));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.pipelineIdentifier));
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetStepTypeToYamlTypeMapping() {
    Map<ExecutionNodeType, String> stepTypeToYamlTypeMapping =
        ngPipelineExecutionService.getStepTypeToYamlTypeMapping();

    assertThat(stepTypeToYamlTypeMapping.size()).isEqualTo(ExecutionNodeType.values().length);
    assertThat(stepTypeToYamlTypeMapping.get(ExecutionNodeType.DEPLOYMENT_STAGE_STEP))
        .isEqualTo(ExecutionNodeType.DEPLOYMENT_STAGE_STEP.getYamlType());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testCreatePipelineExecution() {
    NgPipeline ngPipeline = NgPipeline.builder().identifier("identifier").name("name").build();
    CDPipelineSetupParameters cdPipelineSetupParameters =
        CDPipelineSetupParameters.builder().inputSetPipelineYaml("inputSet").ngPipeline(ngPipeline).build();
    PlanExecution planExecution =
        PlanExecution.builder().uuid("planExecutionUuid").plan(Plan.builder().nodes(new ArrayList<>()).build()).build();
    ArgumentCaptor<PipelineExecutionSummary> pipelineExecutionSummaryArgumentCaptor =
        ArgumentCaptor.forClass(PipelineExecutionSummary.class);
    PipelineExecutionSummary pipelineExecutionSummary =
        PipelineExecutionSummary.builder()
            .accountIdentifier(ACCOUNT_ID)
            .orgIdentifier(ORG_ID)
            .projectIdentifier(PROJECT_ID)
            .pipelineName(ngPipeline.getName())
            .pipelineIdentifier(ngPipeline.getIdentifier())
            .executionStatus(ExecutionStatus.RUNNING)
            .triggerInfo(ExecutionTriggerInfo.newBuilder()
                             .setTriggerType(MANUAL)
                             .setTriggeredBy(TriggeredBy.newBuilder()
                                                 .setUuid("lv0euRhKRCyiXWzS7pOg6g")
                                                 .putExtraInfo("email", "admin@harness.io")
                                                 .setIdentifier("Admin")
                                                 .build())
                             .build())
            .planExecutionId(planExecution.getUuid())
            .startedAt(planExecution.getStartTs())
            .inputSetYaml("inputSet")
            .build();

    when(pipelineExecutionRepository.save(any(PipelineExecutionSummary.class)))
        .thenReturn(PipelineExecutionSummary.builder().build());

    ngPipelineExecutionService.createPipelineExecutionSummary(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, planExecution, cdPipelineSetupParameters);

    verify(pipelineExecutionRepository).save(pipelineExecutionSummaryArgumentCaptor.capture());

    assertThat(pipelineExecutionSummaryArgumentCaptor.getValue()).isEqualTo(pipelineExecutionSummary);
  }
}
