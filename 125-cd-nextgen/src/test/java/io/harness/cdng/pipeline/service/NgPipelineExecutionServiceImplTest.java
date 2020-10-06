package io.harness.cdng.pipeline.service;

import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.utils.PageTestUtils.getPage;
import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.utils.Lists;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.cdng.pipeline.executions.PipelineExecutionHelper;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummaryFilter;
import io.harness.cdng.pipeline.executions.repositories.PipelineExecutionRepository;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionServiceImpl;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.executions.beans.ExecutionGraph;
import io.harness.executions.mapper.ExecutionGraphMapper;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.HashMap;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExecutionGraphMapper.class})
public class NgPipelineExecutionServiceImplTest extends CategoryTest {
  public static final String ACCOUNT_ID = "accountId";
  public static final String ORG_ID = "orgId";
  public static final String PROJECT_ID = "projectId";
  public static final String PLAN_EXECUTION_ID = "projectId";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private GraphGenerationService graphGenerationService;
  @Mock private PipelineExecutionRepository pipelineExecutionRepository;
  @Mock private PipelineService pipelineService;
  @Mock private PipelineExecutionHelper pipelineExecutionHelper;
  @InjectMocks
  private NgPipelineExecutionServiceImpl ngPipelineExecutionService = spy(new NgPipelineExecutionServiceImpl());

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetPipelineExecutionDetail() {
    shouldFailIfNodeForStageIdentifierNotFound();
    shouldReturnStageGraph();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testUpdateStatusForGivenStageNode() {
    NodeExecution nodeExecution =
        NodeExecution.builder().node(PlanNode.builder().group(StepOutcomeGroup.STAGE.name()).build()).build();
    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder().build();
    doReturn(pipelineExecutionSummary)
        .when(ngPipelineExecutionService)
        .getByPlanExecutionId(ACCOUNT_ID, ORG_ID, PROJECT_ID, PLAN_EXECUTION_ID);

    ngPipelineExecutionService.updateStatusForGivenNode(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, PLAN_EXECUTION_ID, nodeExecution);

    verify(pipelineExecutionHelper).updateStageExecutionStatus(pipelineExecutionSummary, nodeExecution);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testUpdateStatusForPipelineNode() {
    NodeExecution nodeExecution =
        NodeExecution.builder().node(PlanNode.builder().group(StepOutcomeGroup.PIPELINE.name()).build()).build();
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
  public void testGetExecutions() {
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(pipelineExecutionRepository.findAll(any(Criteria.class), any(Pageable.class)))
        .thenReturn(getPage(emptyList(), 0));
    when(pipelineService.getPipelineIdentifierToName(any(), anyString(), anyString(), any()))
        .thenReturn(new HashMap<>());
    PipelineExecutionSummaryFilter pipelineExecutionSummaryFilter =
        PipelineExecutionSummaryFilter.builder()
            .executionStatuses(Lists.newArrayList(ExecutionStatus.EXPIRED))
            .endTime(18L)
            .searchTerm("searchTerm")
            .envIdentifiers(Lists.newArrayList("envId"))
            .serviceIdentifiers(Lists.newArrayList("serviceId"))
            .environmentTypes(EnvironmentType.Production)
            .startTime(0L)
            .build();

    Page<PipelineExecutionSummary> pipelineExecutionSummaries = ngPipelineExecutionService.getExecutions(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, Pageable.unpaged(), pipelineExecutionSummaryFilter);

    verify(pipelineExecutionRepository).findAll(criteriaArgumentCaptor.capture(), any(Pageable.class));
    verify(pipelineService).getPipelineIdentifierToName(any(), anyString(), anyString(), any());

    Criteria criteria = criteriaArgumentCaptor.getValue();
    Document criteriaObject = criteria.getCriteriaObject();

    assertEquals(10, criteriaObject.size());
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.executionStatus));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.environmentTypes));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.startedAt));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.endedAt));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.envIdentifiers));
    assertTrue(criteriaObject.containsKey(PipelineExecutionSummaryKeys.serviceIdentifiers));
  }

  private void shouldReturnStageGraph() {
    NodeExecution stageNodeExecution =
        NodeExecution.builder().node(PlanNode.builder().uuid("planNodeId").build()).build();
    doReturn(Optional.of(PipelineExecutionSummary.builder().build()))
        .when(pipelineExecutionRepository)
        .findByPlanExecutionId(any());
    doReturn(Optional.of(stageNodeExecution))
        .when(nodeExecutionService)
        .getByNodeIdentifier("stageId", "planExecutionId");
    OrchestrationGraphDTO orchestrationGraph = OrchestrationGraphDTO.builder().build();
    doReturn(orchestrationGraph)
        .when(graphGenerationService)
        .generatePartialOrchestrationGraphFromSetupNodeId("planNodeId", "planExecutionId");
    PowerMockito.mockStatic(ExecutionGraphMapper.class);
    ExecutionGraph executionGraph = ExecutionGraph.builder().build();
    when(ExecutionGraphMapper.toExecutionGraph(orchestrationGraph)).thenReturn(executionGraph);

    PipelineExecutionDetail pipelineExecutionDetail =
        ngPipelineExecutionService.getPipelineExecutionDetail("planExecutionId", "stageId");
    assertThat(pipelineExecutionDetail.getStageGraph()).isEqualTo(executionGraph);
  }

  private void shouldFailIfNodeForStageIdentifierNotFound() {
    doReturn(Optional.empty()).when(nodeExecutionService).getByNodeIdentifier(anyString(), anyString());
    assertThatThrownBy(() -> ngPipelineExecutionService.getPipelineExecutionDetail("planId", "stageId"))
        .isInstanceOf(InvalidRequestException.class);
  }
}