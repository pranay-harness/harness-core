package io.harness.executionplan.utils;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.executionplan.stepsdependency.StepDependencyInstructor;
import io.harness.executionplan.stepsdependency.instructors.ExpressionStepDependencyInstructor;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class StepDependencyInfoUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetInstructorsList() {
    ExecutionPlanCreationContext context = ExecutionPlanCreationContextImpl.builder().build();
    List<StepDependencyInstructor> instructorsList = StepDependencyInfoUtils.getInstructorsList(context);

    assertThat(instructorsList.isEmpty()).isEqualTo(true);

    StepDependencyInstructor instructor = ExpressionStepDependencyInstructor.builder().key("EXPRESSION").build();
    StepDependencyInfoUtils.addInstructor(instructor, context);
    instructorsList = StepDependencyInfoUtils.getInstructorsList(context);
    assertThat(instructorsList.size()).isEqualTo(1);
    assertThat(instructorsList.get(0)).isEqualTo(instructor);
  }
}
