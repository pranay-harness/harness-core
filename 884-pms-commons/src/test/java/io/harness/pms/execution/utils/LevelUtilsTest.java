package io.harness.pms.execution.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class LevelUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testBuildLevelFromPlanNode() {
    Level level = LevelUtils.buildLevelFromPlanNode("rid1",
        PlanNodeProto.newBuilder()
            .setUuid("uuid")
            .setIdentifier("i1")
            .setSkipExpressionChain(false)
            .setGroup("g")
            .setStepType(StepType.newBuilder().setType("st").setStepCategory(StepCategory.STEP).build())
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

    level = LevelUtils.buildLevelFromPlanNode("rid2", 1,
        PlanNodeProto.newBuilder()
            .setUuid("uuid")
            .setIdentifier("i2")
            .setSkipExpressionChain(true)
            .setStepType(StepType.newBuilder().setType("st").setStepCategory(StepCategory.STEP).build())
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

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testIsStageLevel() {
    assertThat(
        LevelUtils.isStageLevel(
            Level.newBuilder().setStepType(StepType.newBuilder().setStepCategory(StepCategory.STAGE).build()).build()))
        .isTrue();
    for (StepCategory category : StepCategory.values()) {
      if (category == StepCategory.STAGE || category == StepCategory.UNRECOGNIZED) {
        continue;
      }
      assertThat(LevelUtils.isStageLevel(
                     Level.newBuilder().setStepType(StepType.newBuilder().setStepCategory(category).build()).build()))
          .isFalse();
    }
  }
}
