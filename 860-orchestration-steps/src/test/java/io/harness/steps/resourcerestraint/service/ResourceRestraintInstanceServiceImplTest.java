/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.resourcerestraint.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.distribution.constraint.Consumer.State.FINISHED;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationStepsTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.repositories.ResourceRestraintInstanceRepository;
import io.harness.rule.Owner;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.testlib.RealMongo;

import com.google.inject.Inject;
import io.fabric8.utils.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class ResourceRestraintInstanceServiceImplTest extends OrchestrationStepsTestBase {
  private static final String PLAN = "PLAN";
  private static final String OTHER = "OTHER";
  @Inject private ResourceRestraintInstanceRepository restraintInstanceRepository;

  @Mock private PlanExecutionService planExecutionService;
  @Mock private NodeExecutionService nodeExecutionService;
  @Inject @InjectMocks private ResourceRestraintInstanceService resourceRestraintInstanceService;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldTestSave() {
    ResourceRestraintInstance instance = ResourceRestraintInstance.builder()
                                             .releaseEntityId(generateUuid())
                                             .releaseEntityType("PLAN")
                                             .order(1)
                                             .permits(1)
                                             .resourceRestraintId(generateUuid())
                                             .state(ACTIVE)
                                             .build();

    ResourceRestraintInstance savedInstance = resourceRestraintInstanceService.save(instance);
    assertThat(savedInstance).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldTestActivateBlockedInstance() {
    ResourceRestraintInstance instance = ResourceRestraintInstance.builder()
                                             .releaseEntityId(generateUuid())
                                             .releaseEntityType("PLAN")
                                             .resourceUnit(generateUuid())
                                             .order(1)
                                             .permits(1)
                                             .resourceRestraintId(generateUuid())
                                             .state(BLOCKED)
                                             .build();
    ResourceRestraintInstance savedInstance = resourceRestraintInstanceService.save(instance);
    assertThat(savedInstance).isNotNull();

    ResourceRestraintInstance updatedInstance = resourceRestraintInstanceService.activateBlockedInstance(
        savedInstance.getUuid(), savedInstance.getResourceUnit());
    assertThat(updatedInstance).isNotNull();
    assertThat(updatedInstance.getState()).isEqualTo(ACTIVE);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldTestActivateBlockedInstance_InvalidRequestException() {
    ResourceRestraintInstance instance = ResourceRestraintInstance.builder()
                                             .releaseEntityId(generateUuid())
                                             .releaseEntityType("PLAN")
                                             .resourceUnit(generateUuid())
                                             .order(1)
                                             .permits(1)
                                             .resourceRestraintId(generateUuid())
                                             .state(BLOCKED)
                                             .build();
    ResourceRestraintInstance savedInstance = resourceRestraintInstanceService.save(instance);
    assertThat(savedInstance).isNotNull();

    assertThatThrownBy(
        () -> resourceRestraintInstanceService.activateBlockedInstance(generateUuid(), savedInstance.getResourceUnit()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageStartingWith("Cannot find ResourceRestraintInstance with id");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldTestFinishActiveInstance() {
    ResourceRestraintInstance instance = ResourceRestraintInstance.builder()
                                             .releaseEntityId(generateUuid())
                                             .releaseEntityType("PLAN")
                                             .resourceUnit(generateUuid())
                                             .order(1)
                                             .permits(1)
                                             .resourceRestraintId(generateUuid())
                                             .state(ACTIVE)
                                             .build();
    ResourceRestraintInstance savedInstance = resourceRestraintInstanceService.save(instance);
    assertThat(savedInstance).isNotNull();

    ResourceRestraintInstance updatedInstance =
        resourceRestraintInstanceService.finishInstance(savedInstance.getUuid(), savedInstance.getResourceUnit());
    assertThat(updatedInstance).isNotNull();
    assertThat(updatedInstance.getState()).isEqualTo(FINISHED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldTestFinishActiveInstance_InvalidRequestException() {
    ResourceRestraintInstance instance = ResourceRestraintInstance.builder()
                                             .releaseEntityId(generateUuid())
                                             .releaseEntityType("PLAN")
                                             .resourceUnit(generateUuid())
                                             .order(1)
                                             .permits(1)
                                             .resourceRestraintId(generateUuid())
                                             .state(ACTIVE)
                                             .build();
    ResourceRestraintInstance savedInstance = resourceRestraintInstanceService.save(instance);
    assertThat(savedInstance).isNotNull();

    assertThatThrownBy(
        () -> resourceRestraintInstanceService.finishInstance(generateUuid(), savedInstance.getResourceUnit()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageStartingWith("Cannot find ResourceRestraintInstance with id");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldUpdateActiveConstraintsForInstance_ForPlan() {
    ResourceRestraintInstance instance = saveInstance(BLOCKED, PLAN);

    when(planExecutionService.get(any())).thenReturn(PlanExecution.builder().status(Status.SUCCEEDED).build());

    boolean isUpdated = resourceRestraintInstanceService.updateActiveConstraintsForInstance(instance);
    assertThat(isUpdated).isTrue();

    Optional<ResourceRestraintInstance> updatedInstance = restraintInstanceRepository.findById(instance.getUuid());
    assertThat(updatedInstance.isPresent()).isTrue();
    assertThat(updatedInstance.get().getState()).isEqualTo(FINISHED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldUpdateActiveConstraintsForInstance_ForPlan_InvalidRequestException() {
    ResourceRestraintInstance instance = saveInstance(BLOCKED, PLAN);

    when(planExecutionService.get(any())).thenThrow(new InvalidRequestException(""));

    boolean isUpdated = resourceRestraintInstanceService.updateActiveConstraintsForInstance(instance);
    assertThat(isUpdated).isFalse();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldUpdateActiveConstraintsForInstance_ForOther() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(generateUuid())
                            .addAllLevels(Collections.singletonList(
                                Level.newBuilder().setRuntimeId(generateUuid()).setSetupId(generateUuid()).build()))
                            .build();
    ResourceRestraintInstance instance = saveInstance(BLOCKED, OTHER);

    when(nodeExecutionService.getByPlanNodeUuid(any(), any()))
        .thenReturn(NodeExecution.builder()
                        .ambiance(ambiance)
                        .node(PlanNodeProto.newBuilder()
                                  .setUuid(generateUuid())
                                  .setStepType(
                                      StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                                  .setName("dummy")
                                  .setIdentifier("dummy")
                                  .build())
                        .mode(ExecutionMode.SYNC)
                        .status(Status.SUCCEEDED)
                        .build());

    boolean isUpdated = resourceRestraintInstanceService.updateActiveConstraintsForInstance(instance);
    assertThat(isUpdated).isTrue();

    Optional<ResourceRestraintInstance> updatedInstance = restraintInstanceRepository.findById(instance.getUuid());
    assertThat(updatedInstance.isPresent()).isTrue();
    assertThat(updatedInstance.get().getState()).isEqualTo(FINISHED);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldUpdateActiveConstraintsForInstance_ForOther_InvalidRequestException() {
    ResourceRestraintInstance instance = saveInstance(BLOCKED, OTHER);

    when(nodeExecutionService.getByPlanNodeUuid(any(), any())).thenThrow(new InvalidRequestException(""));

    boolean isUpdated = resourceRestraintInstanceService.updateActiveConstraintsForInstance(instance);
    assertThat(isUpdated).isFalse();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldGetAllByRestraintIdAndResourceUnitAndStates() {
    ResourceRestraintInstance instance = saveInstance(ACTIVE, PLAN);

    List<ResourceRestraintInstance> instances =
        resourceRestraintInstanceService.getAllByRestraintIdAndResourceUnitAndStates(
            instance.getResourceRestraintId(), instance.getResourceUnit(), Lists.newArrayList(ACTIVE));

    assertThat(instances).isNotEmpty();
    assertThat(instances.size()).isEqualTo(1);
    assertThat(instances).contains(instance);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldGetMaxOrder() {
    String resourceRestraintId = generateUuid();
    ResourceRestraintInstance instance = saveInstance(resourceRestraintId, ACTIVE, PLAN, 1);
    saveInstance(resourceRestraintId, ACTIVE, PLAN, 2);

    int maxOrder = resourceRestraintInstanceService.getMaxOrder(instance.getResourceRestraintId());
    assertThat(maxOrder).isEqualTo(2);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldGetAllCurrentlyAcquiredPermits() {
    String releaseEntityId = generateUuid();
    ResourceRestraintInstance instance = saveInstance(releaseEntityId);
    saveInstance(releaseEntityId);

    int maxOrder = resourceRestraintInstanceService.getAllCurrentlyAcquiredPermits(
        instance.getReleaseEntityType(), instance.getReleaseEntityId());
    assertThat(maxOrder).isEqualTo(2);
  }

  private ResourceRestraintInstance saveInstance(String releaseEntityId) {
    return saveInstance(generateUuid(), ACTIVE, PLAN, releaseEntityId, 1);
  }

  private ResourceRestraintInstance saveInstance(State state, String releaseEntityType) {
    return saveInstance(generateUuid(), state, releaseEntityType,
        releaseEntityType.equals(PLAN) ? generateUuid() : generateUuid() + '|' + generateUuid(), 1);
  }

  private ResourceRestraintInstance saveInstance(
      String resourceRestraintId, State state, String releaseEntityType, int order) {
    return saveInstance(resourceRestraintId, state, releaseEntityType,
        releaseEntityType.equals(PLAN) ? generateUuid() : generateUuid() + '|' + generateUuid(), order);
  }

  private ResourceRestraintInstance saveInstance(
      String resourceRestraintId, State state, String releaseEntityType, String releaseEntityId, int order) {
    ResourceRestraintInstance instance = ResourceRestraintInstance.builder()
                                             .releaseEntityId(releaseEntityId)
                                             .releaseEntityType(releaseEntityType)
                                             .resourceUnit(generateUuid())
                                             .order(order)
                                             .permits(1)
                                             .resourceRestraintId(resourceRestraintId)
                                             .state(state)
                                             .build();
    ResourceRestraintInstance savedInstance = resourceRestraintInstanceService.save(instance);
    assertThat(savedInstance).isNotNull();

    return savedInstance;
  }
}
