package io.harness.pms.barriers.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.pms.barriers.beans.BarrierExecutionInfo;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.rule.Owner;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.beans.StageDetail;
import io.harness.steps.barriers.service.BarrierService;

import com.google.common.collect.ImmutableSet;
import io.fabric8.utils.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;

public class PMSBarrierServiceTest extends PipelineServiceTestBase {
  @Mock private BarrierService barrierService;
  @Mock private NodeExecutionService nodeExecutionService;
  private PMSBarrierServiceImpl pmsBarrierService;

  @Before
  public void setUp() {
    pmsBarrierService = new PMSBarrierServiceImpl(nodeExecutionService, barrierService);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestGetBarrierExecutionInfoList() {
    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build();
    NodeExecution stageNode = NodeExecution.builder()
                                  .uuid("stageNode")
                                  .status(Status.RUNNING)
                                  .parentId(generateUuid())
                                  .node(PlanNodeProto.newBuilder().setIdentifier(generateUuid()).build())
                                  .ambiance(ambiance)
                                  .version(1L)
                                  .build();

    BarrierExecutionInstance instance1 =
        BarrierExecutionInstance.builder()
            .uuid(generateUuid())
            .name(generateUuid())
            .planNodeId(generateUuid())
            .barrierState(STANDING)
            .barrierGroupId(generateUuid())
            .identifier(generateUuid())
            .planExecutionId(ambiance.getPlanExecutionId())
            .planNodeId(generateUuid())
            .setupInfo(BarrierSetupInfo.builder()
                           .stages(Sets.newSet(StageDetail.builder()
                                                   .name(stageNode.getNode().getName())
                                                   .identifier(stageNode.getNode().getIdentifier())
                                                   .build()))
                           .build())
            .build();

    when(nodeExecutionService.getByPlanNodeUuid(stageNode.getUuid(), ambiance.getPlanExecutionId()))
        .thenReturn(stageNode);

    when(barrierService.findByStageIdentifierAndPlanExecutionIdAnsStateIn(anyString(), anyString(), anySet()))
        .thenReturn(Lists.newArrayList(instance1));

    when(nodeExecutionService.getByPlanNodeUuid(instance1.getPlanNodeId(), ambiance.getPlanExecutionId()))
        .thenThrow(new InvalidRequestException("Exception"));

    List<BarrierExecutionInfo> barrierExecutionInfoList =
        pmsBarrierService.getBarrierExecutionInfoList(stageNode.getUuid(), ambiance.getPlanExecutionId());

    assertThat(barrierExecutionInfoList).isNotNull();
    assertThat(barrierExecutionInfoList.size()).isEqualTo(1);
    assertThat(barrierExecutionInfoList.get(0))
        .isEqualTo(BarrierExecutionInfo.builder()
                       .name(instance1.getName())
                       .identifier(instance1.getIdentifier())
                       .startedAt(0)
                       .started(false)
                       .timeoutIn(0)
                       .stages(ImmutableSet.of(StageDetail.builder()
                                                   .identifier(stageNode.getNode().getIdentifier())
                                                   .name(stageNode.getNode().getName())
                                                   .build()))
                       .build());
  }
}
