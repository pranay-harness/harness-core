package io.harness.engine.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PmsLevelUtilsTest extends OrchestrationTestBase {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testBuildLevelFromPlanNode() {
    Level level = PmsLevelUtils.buildLevelFromPlanNode("rid1",
        PlanNode.builder()
            .uuid("uuid")
            .identifier("i1")
            .skipExpressionChain(false)
            .group("g")
            .stepType(StepType.newBuilder().setType("st").setStepCategory(StepCategory.STEP).build())
            .build());
    assertThat(level.getSetupId()).isEqualTo("uuid");
    assertThat(level.getRuntimeId()).isEqualTo("rid1");
    assertThat(level.getIdentifier()).isEqualTo("i1");
    assertThat(level.getRetryIndex()).isEqualTo(0);
    assertThat(level.getSkipExpressionChain()).isEqualTo(false);
    assertThat(level.getGroup()).isEqualTo("g");
    assertThat(level.getStepType().getType()).isEqualTo("st");
    assertThat(level.getStepType().getStepCategory()).isEqualTo(StepCategory.STEP);
    assertThat(level.getStartTs()).isGreaterThan(0);

    level = PmsLevelUtils.buildLevelFromPlanNode("rid2", 1,
        PlanNode.builder()
            .uuid("uuid")
            .identifier("i2")
            .skipExpressionChain(true)
            .stepType(StepType.newBuilder().setType("st").setStepCategory(StepCategory.STEP).build())
            .build());
    assertThat(level.getSetupId()).isEqualTo("uuid");
    assertThat(level.getRuntimeId()).isEqualTo("rid2");
    assertThat(level.getIdentifier()).isEqualTo("i2");
    assertThat(level.getRetryIndex()).isEqualTo(1);
    assertThat(level.getSkipExpressionChain()).isEqualTo(true);
    assertThat(level.getGroup()).isBlank();
    assertThat(level.getStepType().getType()).isEqualTo("st");
    assertThat(level.getStepType().getStepCategory()).isEqualTo(StepCategory.STEP);
    assertThat(level.getStartTs()).isGreaterThan(0);
  }
}