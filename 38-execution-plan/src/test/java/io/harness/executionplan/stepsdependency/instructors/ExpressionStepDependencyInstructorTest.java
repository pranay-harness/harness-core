package io.harness.executionplan.stepsdependency.instructors;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExpressionStepDependencyInstructorTest extends CategoryTest {
  ExpressionStepDependencyInstructor instructor = ExpressionStepDependencyInstructor.builder().key("TEST").build();

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testSupports() {
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("TEST").build();
    ExecutionPlanCreationContext context = ExecutionPlanCreationContextImpl.builder().build();
    boolean supports = instructor.supports(spec, context);
    assertThat(supports).isEqualTo(true);
    assertThat(instructor.supports(null, context)).isEqualTo(false);
    instructor.attachDependency(spec, null, context);

    assertThat(instructor.getKey()).isEqualTo("TEST");
  }
}
