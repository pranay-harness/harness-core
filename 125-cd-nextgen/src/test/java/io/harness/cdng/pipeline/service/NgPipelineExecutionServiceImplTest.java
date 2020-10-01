package io.harness.cdng.pipeline.service;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.cdng.pipeline.executions.repositories.PipelineExecutionRepository;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionServiceImpl;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.executions.beans.ExecutionGraph;
import io.harness.executions.mapper.ExecutionGraphMapper;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.service.GraphGenerationService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExecutionGraphMapper.class})
public class NgPipelineExecutionServiceImplTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private GraphGenerationService graphGenerationService;
  @Mock private PipelineExecutionRepository pipelineExecutionRepository;
  @InjectMocks private NgPipelineExecutionServiceImpl ngPipelineExecutionService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetPipelineExecutionDetail() {
    shouldFailIfNodeForStageIdentifierNotFound();
    shouldReturnStageGraph();
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
        .generatePartialOrchestrationGraph("planNodeId", "planExecutionId");
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