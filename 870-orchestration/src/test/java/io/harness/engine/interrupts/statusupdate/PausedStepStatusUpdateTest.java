/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.interrupts.statusupdate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
public class PausedStepStatusUpdateTest extends OrchestrationTestBase {
  @Inject PausedStepStatusUpdate stepStatusUpdate;
  @Inject PlanExecutionMetadataService planExecutionMetadataService;
  @Inject MongoTemplate mongoTemplate;
  @Inject Injector injector;

  @Before
  public void setUp() throws Exception {
    NodeExecutionServiceImpl nodeExecutionService =
        (NodeExecutionServiceImpl) injector.getInstance(NodeExecutionService.class);
    nodeExecutionService.getStepStatusUpdateSubject().register(injector.getInstance(PlanExecutionService.class));
  }

  /**
   * Setup:
   *
   * PlanExecution : RUNNING
   *
   * pipeline (RUNNING)
   *  - stage (RUNNING)
   *    -fork (RUNNING)
   *      -child1 (PAUSED) ----> This gets paused
   *      -child2 (SUCCEEDED)
   *      -child3 (SUCCEEDED)
   *
   * PausedStepStatusUpdate should mark the PlanExecution as PAUSED along with all the parents
   */

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStatusUpdateIntervention() {
    String planExecutionId = generateUuid();
    String pipelineNodeId = generateUuid() + "plNode";
    String stageNodeId = generateUuid() + "stageNode";
    String forkNodeId = generateUuid() + "forkNode";
    String child1Id = generateUuid() + "child1Node";
    String child2Id = generateUuid() + "child2Node";
    String child3Id = generateUuid() + "child3Node";
    PlanExecution planExecution = PlanExecution.builder().uuid(planExecutionId).status(Status.RUNNING).build();
    PlanExecutionMetadata planExecutionMetadata =
        PlanExecutionMetadata.builder().planExecutionId(planExecutionId).build();
    planExecutionMetadataService.save(planExecutionMetadata);

    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(planExecutionId).build();
    PlanNodeProto planNode = PlanNodeProto.newBuilder().build();

    NodeExecution pipelineNode =
        NodeExecution.builder().uuid(pipelineNodeId).status(Status.RUNNING).node(planNode).ambiance(ambiance).build();
    NodeExecution stageNode = NodeExecution.builder()
                                  .uuid(stageNodeId)
                                  .status(Status.RUNNING)
                                  .parentId(pipelineNode.getUuid())
                                  .ambiance(ambiance)
                                  .node(planNode)
                                  .build();
    NodeExecution forkNode = NodeExecution.builder()
                                 .uuid(forkNodeId)
                                 .status(Status.RUNNING)
                                 .parentId(stageNode.getUuid())
                                 .ambiance(ambiance)
                                 .node(planNode)
                                 .build();
    // This node is getting transitioned to SUCCEEDED from MANUAL_INTERVENTION
    NodeExecution child1 = NodeExecution.builder()
                               .uuid(child1Id)
                               .status(Status.PAUSED)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .node(planNode)
                               .build();
    NodeExecution child2 = NodeExecution.builder()
                               .uuid(child2Id)
                               .status(Status.SUCCEEDED)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .node(planNode)
                               .build();
    NodeExecution child3 = NodeExecution.builder()
                               .uuid(child3Id)
                               .status(Status.SUCCEEDED)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .node(planNode)
                               .build();

    mongoTemplate.save(planExecution);
    mongoTemplate.save(pipelineNode);
    mongoTemplate.save(stageNode);
    mongoTemplate.save(forkNode);
    mongoTemplate.save(child1);
    mongoTemplate.save(child2);
    mongoTemplate.save(child3);

    stepStatusUpdate.handleNodeStatusUpdate(NodeUpdateInfo.builder().nodeExecution(child1).build());

    PlanExecution updated =
        mongoTemplate.findOne(query(where(PlanExecutionKeys.uuid).is(planExecutionId)), PlanExecution.class);
    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo(Status.PAUSED);

    NodeExecution pipelineNodeEx =
        mongoTemplate.findOne(query(where(NodeExecutionKeys.uuid).is(pipelineNodeId)), NodeExecution.class);
    assertThat(pipelineNodeEx).isNotNull();
    assertThat(pipelineNodeEx.getStatus()).isEqualTo(Status.PAUSED);

    NodeExecution stageNodeEx =
        mongoTemplate.findOne(query(where(NodeExecutionKeys.uuid).is(stageNodeId)), NodeExecution.class);
    assertThat(stageNodeEx).isNotNull();
    assertThat(stageNodeEx.getStatus()).isEqualTo(Status.PAUSED);

    NodeExecution forkNodeEx =
        mongoTemplate.findOne(query(where(NodeExecutionKeys.uuid).is(forkNodeId)), NodeExecution.class);
    assertThat(forkNodeEx).isNotNull();
    assertThat(forkNodeEx.getStatus()).isEqualTo(Status.PAUSED);
  }

  /**
   * Setup:
   *
   * PlanExecution : INTERVENTION_WAITING
   *
   * pipeline (running)
   *  - stage (running)
   *    -fork (running)
   *      -child1 (PAUSED) ----> This gets paused
   *      -child2 (SUCCEEDED)
   *      -child3 (RUNNING)
   *    -fork (running)
   *      -child4 (running)
   *
   * PausedStepStatusUpdate should mark the Fork1 as Paused others should be running
   */

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStatusUpdateRunning() {
    String planExecutionId = generateUuid();
    String pipelineNodeId = generateUuid() + "plNode";
    String stageNodeId = generateUuid() + "stageNode";
    String forkNodeId = generateUuid() + "forkNode";
    String child1Id = generateUuid() + "child1Node";
    String child2Id = generateUuid() + "child2Node";
    String child3Id = generateUuid() + "child3Node";
    String fork2Id = generateUuid() + "fork2Node";
    String child4Id = generateUuid() + "child4Node";

    PlanExecutionMetadata planExecutionMetadata =
        PlanExecutionMetadata.builder().planExecutionId(planExecutionId).build();
    planExecutionMetadataService.save(planExecutionMetadata);
    PlanExecution planExecution = PlanExecution.builder().uuid(planExecutionId).status(Status.RUNNING).build();
    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(planExecutionId).build();
    PlanNodeProto planNode = PlanNodeProto.newBuilder().build();

    NodeExecution pipelineNode =
        NodeExecution.builder().uuid(pipelineNodeId).status(Status.RUNNING).ambiance(ambiance).node(planNode).build();
    NodeExecution stageNode = NodeExecution.builder()
                                  .uuid(stageNodeId)
                                  .status(Status.RUNNING)
                                  .parentId(pipelineNode.getUuid())
                                  .ambiance(ambiance)
                                  .node(planNode)
                                  .build();
    NodeExecution forkNode = NodeExecution.builder()
                                 .uuid(forkNodeId)
                                 .status(Status.RUNNING)
                                 .parentId(stageNode.getUuid())
                                 .ambiance(ambiance)
                                 .node(planNode)
                                 .build();
    // This node is getting transitioned to SUCCEEDED from MANUAL_INTERVENTION
    NodeExecution child1 = NodeExecution.builder()
                               .uuid(child1Id)
                               .status(Status.PAUSED)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .node(planNode)
                               .build();
    NodeExecution child2 = NodeExecution.builder()
                               .uuid(child2Id)
                               .status(Status.SUCCEEDED)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .node(planNode)
                               .build();
    NodeExecution child3 = NodeExecution.builder()
                               .uuid(child3Id)
                               .status(Status.SUCCEEDED)
                               .parentId(forkNode.getUuid())
                               .ambiance(ambiance)
                               .node(planNode)
                               .build();

    NodeExecution fork2Node = NodeExecution.builder()
                                  .uuid(fork2Id)
                                  .status(Status.RUNNING)
                                  .parentId(stageNode.getUuid())
                                  .ambiance(ambiance)
                                  .node(planNode)
                                  .build();

    NodeExecution child4 = NodeExecution.builder()
                               .uuid(child4Id)
                               .status(Status.RUNNING)
                               .parentId(fork2Node.getUuid())
                               .ambiance(ambiance)
                               .node(planNode)
                               .build();

    mongoTemplate.save(planExecution);
    mongoTemplate.save(pipelineNode);
    mongoTemplate.save(stageNode);
    mongoTemplate.save(forkNode);
    mongoTemplate.save(child1);
    mongoTemplate.save(child2);
    mongoTemplate.save(child3);
    mongoTemplate.save(fork2Node);
    mongoTemplate.save(child4);

    stepStatusUpdate.handleNodeStatusUpdate(NodeUpdateInfo.builder()
                                                .nodeExecution(child1)

                                                .build());

    PlanExecution updated =
        mongoTemplate.findOne(query(where(PlanExecutionKeys.uuid).is(planExecutionId)), PlanExecution.class);
    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo(Status.RUNNING);

    NodeExecution pipelineNodeEx =
        mongoTemplate.findOne(query(where(NodeExecutionKeys.uuid).is(pipelineNodeId)), NodeExecution.class);
    assertThat(pipelineNodeEx).isNotNull();
    assertThat(pipelineNodeEx.getStatus()).isEqualTo(Status.RUNNING);

    NodeExecution stageNodeEx =
        mongoTemplate.findOne(query(where(NodeExecutionKeys.uuid).is(stageNodeId)), NodeExecution.class);
    assertThat(stageNodeEx).isNotNull();
    assertThat(stageNodeEx.getStatus()).isEqualTo(Status.RUNNING);

    NodeExecution forkNodeEx =
        mongoTemplate.findOne(query(where(NodeExecutionKeys.uuid).is(forkNodeId)), NodeExecution.class);
    assertThat(forkNodeEx).isNotNull();
    assertThat(forkNodeEx.getStatus()).isEqualTo(Status.PAUSED);
  }
}
