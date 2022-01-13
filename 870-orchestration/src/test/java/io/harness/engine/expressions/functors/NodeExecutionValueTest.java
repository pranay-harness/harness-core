/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.expressions.functors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.GARVIT;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.steps.TestStepParameters;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.logging.impl.NoOpLog;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeExecutionValueTest extends OrchestrationTestBase {
  @Mock NodeExecutionService nodeExecutionService;
  @Mock PmsOutcomeService pmsOutcomeService;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private JexlEngine engine;
  private Ambiance ambiance;
  NodeExecution nodeExecution1;
  NodeExecution nodeExecution2;
  NodeExecution nodeExecution3;
  NodeExecution nodeExecution4;
  NodeExecution nodeExecution5;
  NodeExecution nodeExecution6;
  NodeExecution nodeExecution7;
  NodeExecution nodeExecution8;

  @Before
  public void setup() {
    String nodeExecution1Id = generateUuid();
    String nodeExecution2Id = generateUuid();
    String nodeExecution3Id = generateUuid();
    String nodeExecution4Id = generateUuid();
    String nodeExecution5Id = generateUuid();
    String nodeExecution6Id = generateUuid();
    String nodeExecution7Id = generateUuid();
    String nodeExecution8Id = generateUuid();
    engine = new JexlBuilder().logger(new NoOpLog()).create();
    ambiance = AmbianceTestUtils.buildAmbiance();

    nodeExecution1 = NodeExecution.builder()
                         .uuid(nodeExecution1Id)
                         .node(preparePlanNode(false, "a"))
                         .resolvedStepParameters(prepareStepParameters("ao"))
                         .build();
    nodeExecution2 = NodeExecution.builder()
                         .uuid(nodeExecution2Id)
                         .node(preparePlanNode(false, "b"))
                         .resolvedStepParameters(prepareStepParameters("bo"))
                         .parentId(nodeExecution1Id)
                         .nextId(nodeExecution1Id)
                         .build();
    nodeExecution3 = NodeExecution.builder()
                         .uuid(nodeExecution3Id)
                         .node(preparePlanNode(true, "c"))
                         .resolvedStepParameters(prepareStepParameters("co"))
                         .parentId(nodeExecution1Id)
                         .previousId(nodeExecution2Id)
                         .build();
    nodeExecution4 = NodeExecution.builder()
                         .uuid(nodeExecution4Id)
                         .node(preparePlanNode(false, "d", "di1", "STAGE"))
                         .parentId(nodeExecution3Id)
                         .nextId(nodeExecution5Id)
                         .build();
    nodeExecution5 = NodeExecution.builder()
                         .uuid(nodeExecution5Id)
                         .node(preparePlanNode(false, "d", "di2"))
                         .resolvedStepParameters(prepareStepParameters("do2"))
                         .parentId(nodeExecution3Id)
                         .previousId(nodeExecution4Id)
                         .build();
    nodeExecution6 = NodeExecution.builder()
                         .uuid(nodeExecution6Id)
                         .node(preparePlanNode(false, "e"))
                         .resolvedStepParameters(prepareStepParameters("eo"))
                         .parentId(nodeExecution4Id)
                         .build();

    nodeExecution7 = NodeExecution.builder()
                         .uuid(nodeExecution7Id)
                         .node(preparePlanNode(false, "f"))
                         .resolvedStepParameters(prepareStepParameters("eo"))
                         .parentId(nodeExecution6Id)
                         .nextId(nodeExecution8Id)
                         .build();

    nodeExecution8 = NodeExecution.builder()
                         .uuid(nodeExecution8Id)
                         .node(preparePlanNode(false, "g"))
                         .resolvedStepParameters(prepareStepParameters("eo"))
                         .parentId(nodeExecution6Id)
                         .previousId(nodeExecution7Id)
                         .build();

    when(nodeExecutionService.get(nodeExecution1.getUuid())).thenReturn(nodeExecution1);
    when(nodeExecutionService.get(nodeExecution2.getUuid())).thenReturn(nodeExecution2);
    when(nodeExecutionService.get(nodeExecution3.getUuid())).thenReturn(nodeExecution3);
    when(nodeExecutionService.get(nodeExecution4.getUuid())).thenReturn(nodeExecution4);
    when(nodeExecutionService.get(nodeExecution5.getUuid())).thenReturn(nodeExecution5);
    when(nodeExecutionService.get(nodeExecution6.getUuid())).thenReturn(nodeExecution6);
    when(nodeExecutionService.get(nodeExecution7.getUuid())).thenReturn(nodeExecution7);
    when(nodeExecutionService.get(nodeExecution8.getUuid())).thenReturn(nodeExecution8);

    String planExecutionId = ambiance.getPlanExecutionId();
    when(nodeExecutionService.fetchChildrenNodeExecutions(planExecutionId, null))
        .thenReturn(Collections.singletonList(nodeExecution1));
    when(nodeExecutionService.fetchChildrenNodeExecutions(planExecutionId, nodeExecution1.getUuid()))
        .thenReturn(asList(nodeExecution2, nodeExecution3));
    when(nodeExecutionService.fetchChildrenNodeExecutions(planExecutionId, nodeExecution3.getUuid()))
        .thenReturn(asList(nodeExecution4, nodeExecution5));
    when(nodeExecutionService.fetchChildrenNodeExecutions(planExecutionId, nodeExecution4.getUuid()))
        .thenReturn(Collections.singletonList(nodeExecution6));
    when(nodeExecutionService.fetchChildrenNodeExecutions(planExecutionId, nodeExecution6.getUuid()))
        .thenReturn(asList(nodeExecution7, nodeExecution8));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testNodeExecutionChildFunctor() {
    Ambiance newAmbiance =
        AmbianceUtils.cloneForChild(ambiance, Level.newBuilder().setRuntimeId(nodeExecution1.getUuid()).build());
    NodeExecutionChildFunctor functor =
        NodeExecutionChildFunctor.builder()
            .nodeExecutionsCache(new NodeExecutionsCache(nodeExecutionService, newAmbiance))
            .pmsOutcomeService(pmsOutcomeService)
            .ambiance(newAmbiance)
            .build();
    NodeExecutionMap nodeExecutionMap = (NodeExecutionMap) functor.bind();
    assertThat(engine.getProperty(nodeExecutionMap, "param")).isEqualTo("ao");
    assertThat(engine.getProperty(nodeExecutionMap, "b.param")).isEqualTo("bo");
    assertThat(engine.getProperty(nodeExecutionMap, "d[0].param")).isEqualTo("di1");
    assertThat(engine.getProperty(nodeExecutionMap, "d[1].param")).isEqualTo("do2");
    assertThat(engine.getProperty(nodeExecutionMap, "d[0].e.param")).isEqualTo("eo");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testNodeExecutionAncestorFunctor() {
    Ambiance newAmbiance =
        AmbianceUtils.cloneForChild(ambiance, Level.newBuilder().setRuntimeId(nodeExecution6.getUuid()).build());
    NodeExecutionAncestorFunctor functor =
        NodeExecutionAncestorFunctor.builder()
            .nodeExecutionsCache(new NodeExecutionsCache(nodeExecutionService, newAmbiance))
            .pmsOutcomeService(pmsOutcomeService)
            .ambiance(newAmbiance)
            .groupAliases(ImmutableMap.of("stage", "STAGE"))
            .build();
    assertThat(engine.getProperty(functor, "stage.param")).isEqualTo("di1");
    assertThat(engine.getProperty(functor, "stage.e.param")).isEqualTo("eo");
    assertThat(engine.getProperty(functor, "a.b.param")).isEqualTo("bo");
    assertThat(engine.getProperty(functor, "a.d[0].param")).isEqualTo("di1");
    assertThat(engine.getProperty(functor, "a.d[1].param")).isEqualTo("do2");
    assertThat(engine.getProperty(functor, "a.d[0].e.param")).isEqualTo("eo");
    assertThat(engine.getProperty(functor, "d.param")).isEqualTo("di1");
    assertThat(engine.getProperty(functor, "d.e.param")).isEqualTo("eo");
    assertThat(engine.getProperty(functor, "e.param")).isEqualTo("eo");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testNodeExecutionCurrentStatus() {
    Ambiance newAmbiance =
        AmbianceUtils.cloneForChild(ambiance, Level.newBuilder().setRuntimeId(nodeExecution8.getUuid()).build());
    NodeExecutionAncestorFunctor functor =
        NodeExecutionAncestorFunctor.builder()
            .nodeExecutionsCache(new NodeExecutionsCache(nodeExecutionService, newAmbiance))
            .pmsOutcomeService(pmsOutcomeService)
            .ambiance(newAmbiance)
            .groupAliases(ImmutableMap.of("stage", "STAGE"))
            .build();

    when(nodeExecutionService.findAllChildren(ambiance.getPlanExecutionId(), nodeExecution4.getUuid(), false))
        .thenReturn(asList(nodeExecution8, nodeExecution7, nodeExecution6));

    Reflect.on(nodeExecution4).set(NodeExecutionKeys.status, Status.RUNNING);
    Reflect.on(nodeExecution6).set(NodeExecutionKeys.status, Status.RUNNING);
    Reflect.on(nodeExecution7).set(NodeExecutionKeys.status, Status.SUCCEEDED);
    Reflect.on(nodeExecution8).set(NodeExecutionKeys.status, Status.QUEUED);

    // Check current status for SUCCEEDED
    assertThat(engine.getProperty(functor, "stage.currentStatus")).isEqualTo("SUCCEEDED");

    // Check current status for FAILED
    Reflect.on(nodeExecution7).set(NodeExecutionKeys.status, Status.FAILED);
    assertThat(engine.getProperty(functor, "stage.currentStatus")).isEqualTo("FAILED");

    // Check current status for ERRORED
    Reflect.on(nodeExecution7).set(NodeExecutionKeys.status, Status.ERRORED);
    assertThat(engine.getProperty(functor, "stage.currentStatus")).isEqualTo("ERRORED");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testNodeExecutionQualifiedFunctor() {
    NodeExecutionQualifiedFunctor functor =
        NodeExecutionQualifiedFunctor.builder()
            .nodeExecutionsCache(new NodeExecutionsCache(nodeExecutionService, ambiance))
            .pmsOutcomeService(pmsOutcomeService)
            .ambiance(ambiance)
            .build();
    NodeExecutionMap nodeExecutionMap = (NodeExecutionMap) functor.bind();
    assertThat(engine.getProperty(nodeExecutionMap, "a.b.param")).isEqualTo("bo");
    assertThat(engine.getProperty(nodeExecutionMap, "a.d[0].param")).isEqualTo("di1");
    assertThat(engine.getProperty(nodeExecutionMap, "a.d[1].param")).isEqualTo("do2");
    assertThat(engine.getProperty(nodeExecutionMap, "a.d[0].e.param")).isEqualTo("eo");
  }

  private PlanNodeProto preparePlanNode(boolean skipExpressionChain, String identifier) {
    return preparePlanNode(skipExpressionChain, identifier, identifier + "i");
  }

  private PlanNodeProto preparePlanNode(boolean skipExpressionChain, String identifier, String paramValue) {
    return preparePlanNode(skipExpressionChain, identifier, paramValue, null);
  }

  private PlanNodeProto preparePlanNode(
      boolean skipExpressionChain, String identifier, String paramValue, String groupName) {
    PlanNodeProto.Builder builder =
        PlanNodeProto.newBuilder()
            .setUuid(generateUuid())
            .setName(identifier + "n")
            .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
            .setIdentifier(identifier)
            .setSkipExpressionChain(skipExpressionChain)
            .setStepParameters(RecastOrchestrationUtils.toJson(prepareStepParameters(paramValue)));

    if (!isEmpty(groupName)) {
      builder.setGroup(groupName);
    }
    return builder.build();
  }

  private StepParameters prepareStepParameters(String paramValue) {
    return TestStepParameters.builder().param(paramValue).build();
  }
}
