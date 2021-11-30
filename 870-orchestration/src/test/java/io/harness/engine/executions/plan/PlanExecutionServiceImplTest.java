package io.harness.engine.executions.plan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.PlanExecution.PlanExecutionKeys;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.MLUKIC;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.events.PmsEventMonitoringConstants;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PlanExecutionServiceImplTest extends OrchestrationTestBase {
  @Mock NodeExecutionService nodeExecutionService;
  @Inject @InjectMocks PlanExecutionService planExecutionService;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSave() {
    String planExecutionId = generateUuid();
    PlanExecution savedExecution = planExecutionService.save(PlanExecution.builder().uuid(planExecutionId).build());
    assertThat(savedExecution.getUuid()).isEqualTo(planExecutionId);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestUpdate() {
    String planExecutionId = generateUuid();
    long millis = System.currentTimeMillis();
    PlanExecution savedExecution = planExecutionService.save(PlanExecution.builder().uuid(planExecutionId).build());
    assertThat(savedExecution.getUuid()).isEqualTo(planExecutionId);

    planExecutionService.update(planExecutionId, ops -> ops.set(PlanExecutionKeys.endTs, millis));

    PlanExecution updated = planExecutionService.get(planExecutionId);
    assertThat(updated.getUuid()).isEqualTo(planExecutionId);
    assertThat(updated.getEndTs()).isEqualTo(millis);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestFindAllByPlanExecutionIdIn() {
    String planExecutionId = generateUuid();
    PlanExecution savedExecution = planExecutionService.save(PlanExecution.builder().uuid(planExecutionId).build());
    assertThat(savedExecution.getUuid()).isEqualTo(planExecutionId);

    List<PlanExecution> planExecutions =
        planExecutionService.findAllByPlanExecutionIdIn(ImmutableList.of(planExecutionId));

    assertThat(planExecutions).isNotEmpty();
    assertThat(planExecutions.size()).isEqualTo(1);
    assertThat(planExecutions).extracting(PlanExecution::getUuid).containsExactly(planExecutionId);
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestCalculateStatusExcluding() {
    String excludedNodeExecutionId = generateUuid();
    String planExecutionId = generateUuid();
    PlanExecution savedExecution =
        planExecutionService.save(PlanExecution.builder().uuid(planExecutionId).status(Status.PAUSED).build());
    assertThat(savedExecution.getUuid()).isEqualTo(planExecutionId);

    when(nodeExecutionService.fetchNodeExecutionsWithoutOldRetriesAndStatusIn(
             eq(planExecutionId), eq(EnumSet.noneOf(Status.class))))
        .thenReturn(
            ImmutableList.of(NodeExecution.builder().uuid(excludedNodeExecutionId).status(Status.QUEUED).build(),
                NodeExecution.builder().uuid(generateUuid()).status(Status.RUNNING).build()));

    Status status = planExecutionService.calculateStatusExcluding(planExecutionId, excludedNodeExecutionId);
    assertThat(status).isEqualTo(Status.RUNNING);
  }

  @Test
  @RealMongo
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void shouldTestFindAllByAccountIdAndOrgIdAndProjectIdAndLastUpdatedAtInBetweenTimestamps() {
    String planExecutionId = generateUuid();
    String accountId = "TestAccountId";
    String orgId = "TestOrgId";
    String projectId = "TestProjectId";
    long startTS = System.currentTimeMillis();
    Map<String, String> setupAbstractions = new HashMap<>();
    PlanExecution savedExecution = planExecutionService.save(
        PlanExecution.builder().uuid(planExecutionId).setupAbstractions(setupAbstractions).build());
    assertThat(savedExecution.getUuid()).isEqualTo(planExecutionId);
    assertThat(savedExecution.getSetupAbstractions().get(PmsEventMonitoringConstants.ACCOUNT_ID)).isEqualTo(accountId);
    assertThat(savedExecution.getSetupAbstractions().get(PmsEventMonitoringConstants.ORG_ID)).isEqualTo(orgId);
    assertThat(savedExecution.getSetupAbstractions().get(PmsEventMonitoringConstants.PROJECT_ID)).isEqualTo(projectId);

    long endTS = System.currentTimeMillis();

    List<PlanExecution> planExecutions =
        planExecutionService.findAllByAccountIdAndOrgIdAndProjectIdAndLastUpdatedAtInBetweenTimestamps(
            accountId, orgId, projectId, startTS, endTS);

    assertThat(planExecutions).isNotEmpty();
    assertThat(planExecutions.size()).isEqualTo(1);
    assertThat(planExecutions).extracting(PlanExecution::getUuid).containsExactly(planExecutionId);
    assertThat(planExecutions).extracting(PlanExecution::getSetupAbstractions).containsExactly(setupAbstractions);
  }
}
