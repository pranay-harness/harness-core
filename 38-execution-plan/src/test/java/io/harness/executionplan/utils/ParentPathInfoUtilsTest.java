package io.harness.executionplan.utils;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.executionplan.plancreator.beans.PlanLevelNode;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ParentPathInfoUtilsTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetParentPath() {
    ExecutionPlanCreationContext context = ExecutionPlanCreationContextImpl.builder().build();
    String parentPath = ParentPathInfoUtils.getParentPath(context);
    assertThat(parentPath).isEqualTo("");
    assertThatThrownBy(() -> ParentPathInfoUtils.getParentPath(PlanNodeType.STAGE.name(), context))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("Parent Path has not been initialised");

    PlanLevelNode planLevelNode = PlanLevelNode.builder()
                                      .planNodeType(PlanNodeType.PIPELINE.name())
                                      .identifier(PlanNodeType.PIPELINE.name())
                                      .build();
    ParentPathInfoUtils.addToParentPath(context, planLevelNode);
    planLevelNode =
        PlanLevelNode.builder().planNodeType(PlanNodeType.STAGE.name()).identifier(PlanNodeType.STAGE.name()).build();
    ParentPathInfoUtils.addToParentPath(context, planLevelNode);
    parentPath = ParentPathInfoUtils.getParentPath(context);
    assertThat(parentPath).isEqualTo("PIPELINE.STAGE");
    parentPath = ParentPathInfoUtils.getParentPath("PIPELINE", context);
    assertThat(parentPath).isEqualTo("PIPELINE");

    assertThatThrownBy(() -> ParentPathInfoUtils.getParentPath("PHASE", context))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("PlanNode type doesn't exist in parent path");

    ParentPathInfoUtils.removeFromParentPath(context);
    parentPath = ParentPathInfoUtils.getParentPath(context);
    assertThat(parentPath).isEqualTo("PIPELINE");
  }
}
