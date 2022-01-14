/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.resourceconstraint.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.resourceconstraints.response.ResourceConstraintDetailDTO;
import io.harness.pms.resourceconstraints.response.ResourceConstraintExecutionInfoDTO;
import io.harness.pms.resourceconstraints.service.PMSResourceConstraintServiceImpl;
import io.harness.pms.utils.PmsConstants;
import io.harness.rule.Owner;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.service.ResourceRestraintInstanceService;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class PMSResourceConstraintServiceTest extends PipelineServiceTestBase {
  private static final String ACCOUNT_ID = generateUuid();
  private static final String PLAN_EXECUTION_ID = generateUuid();
  private static final String RESOURCE_UNIT = generateUuid();

  @Mock private ResourceRestraintService resourceRestraintService;
  @Mock private ResourceRestraintInstanceService resourceRestraintInstanceService;
  @Mock private PlanExecutionService planExecutionService;
  private PMSResourceConstraintServiceImpl pmsResourceConstraintService;

  @Before
  public void setUp() {
    pmsResourceConstraintService = new PMSResourceConstraintServiceImpl(
        resourceRestraintService, resourceRestraintInstanceService, planExecutionService);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenResourceConstraintNotFound() {
    when(resourceRestraintService.getByNameAndAccountId(PmsConstants.QUEUING_RC_NAME, ACCOUNT_ID)).thenReturn(null);

    assertThatThrownBy(() -> pmsResourceConstraintService.getResourceConstraintExecutionInfo(ACCOUNT_ID, RESOURCE_UNIT))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(String.format(PMSResourceConstraintServiceImpl.NOT_FOUND_WITH_ARGUMENTS, ACCOUNT_ID));

    verify(resourceRestraintService).getByNameAndAccountId(PmsConstants.QUEUING_RC_NAME, ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldGetResourceConstraintExecutionInfoList() {
    ResourceRestraint resourceConstraint = getResourceConstraint();
    List<ResourceRestraintInstance> restraintInstanceList =
        Lists.newArrayList(ResourceRestraintInstance.builder()
                               .state(State.BLOCKED)
                               .uuid(generateUuid())
                               .resourceRestraintId(resourceConstraint.getUuid())
                               .resourceUnit(RESOURCE_UNIT)
                               .releaseEntityType(PmsConstants.RELEASE_ENTITY_TYPE_PLAN)
                               .releaseEntityId(PLAN_EXECUTION_ID)
                               .order(2)
                               .build(),
            ResourceRestraintInstance.builder()
                .state(State.ACTIVE)
                .uuid(generateUuid())
                .resourceRestraintId(resourceConstraint.getUuid())
                .resourceUnit(RESOURCE_UNIT)
                .releaseEntityType(PmsConstants.RELEASE_ENTITY_TYPE_PLAN)
                .releaseEntityId(PLAN_EXECUTION_ID + "1")
                .order(1)
                .build(),
            ResourceRestraintInstance.builder()
                .state(State.BLOCKED)
                .uuid(generateUuid())
                .resourceRestraintId(resourceConstraint.getUuid())
                .resourceUnit(RESOURCE_UNIT)
                .releaseEntityType(PmsConstants.RELEASE_ENTITY_TYPE_PLAN)
                .releaseEntityId(PLAN_EXECUTION_ID + "2")
                .order(3)
                .build());
    List<PlanExecution> planExecutionList =
        Lists.newArrayList(PlanExecution.builder()
                               .uuid(PLAN_EXECUTION_ID)
                               .metadata(ExecutionMetadata.newBuilder().setPipelineIdentifier("k8s").build())
                               .build(),
            PlanExecution.builder()
                .uuid(PLAN_EXECUTION_ID + "1")
                .metadata(ExecutionMetadata.newBuilder().setPipelineIdentifier("rc-pipeline").build())
                .build(),
            PlanExecution.builder()
                .uuid(PLAN_EXECUTION_ID + "2")
                .metadata(ExecutionMetadata.newBuilder().setPipelineIdentifier("barriers-pipeline").build())
                .build());

    when(resourceRestraintService.getByNameAndAccountId(PmsConstants.QUEUING_RC_NAME, ACCOUNT_ID))
        .thenReturn(resourceConstraint);
    when(resourceRestraintInstanceService.getAllByRestraintIdAndResourceUnitAndStates(
             resourceConstraint.getUuid(), RESOURCE_UNIT, Arrays.asList(ACTIVE, BLOCKED)))
        .thenReturn(restraintInstanceList);
    when(planExecutionService.findAllByPlanExecutionIdIn(any())).thenReturn(planExecutionList);

    ResourceConstraintExecutionInfoDTO response =
        pmsResourceConstraintService.getResourceConstraintExecutionInfo(ACCOUNT_ID, RESOURCE_UNIT);

    assertThat(response).isNotNull();
    assertThat(response.getCapacity()).isEqualTo(resourceConstraint.getCapacity());
    assertThat(response.getName()).isEqualTo(resourceConstraint.getName());
    assertThat(response.getResourceConstraints()).isNotEmpty();
    assertThat(response.getResourceConstraints().size()).isEqualTo(3);
    assertThat(response.getResourceConstraints())
        .containsExactly(ResourceConstraintDetailDTO.builder()
                             .pipelineIdentifier("rc-pipeline")
                             .planExecutionId(PLAN_EXECUTION_ID + "1")
                             .state(ACTIVE)
                             .build(),
            ResourceConstraintDetailDTO.builder()
                .pipelineIdentifier("k8s")
                .planExecutionId(PLAN_EXECUTION_ID)
                .state(BLOCKED)
                .build(),
            ResourceConstraintDetailDTO.builder()
                .pipelineIdentifier("barriers-pipeline")
                .planExecutionId(PLAN_EXECUTION_ID + "2")
                .state(BLOCKED)
                .build());

    verify(resourceRestraintService).getByNameAndAccountId(PmsConstants.QUEUING_RC_NAME, ACCOUNT_ID);
    verify(resourceRestraintInstanceService)
        .getAllByRestraintIdAndResourceUnitAndStates(
            resourceConstraint.getUuid(), RESOURCE_UNIT, Arrays.asList(ACTIVE, BLOCKED));
    verify(planExecutionService).findAllByPlanExecutionIdIn(any());
  }

  private ResourceRestraint getResourceConstraint() {
    return ResourceRestraint.builder()
        .accountId(ACCOUNT_ID)
        .capacity(1)
        .createdAt(System.currentTimeMillis())
        .createdBy(EmbeddedUser.builder().name(ALEXEI).build())
        .harnessOwned(true)
        .name(PmsConstants.QUEUING_RC_NAME)
        .strategy(Strategy.FIFO)
        .uuid(generateUuid())
        .build();
  }
}
