package io.harness.executionplan.stepsdependency.instructors;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.pms.refobjects.RefObject;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OutcomeRefStepDependencyInstructorTest extends CategoryTest {
  OutcomeRefStepDependencyInstructor instructor = OutcomeRefStepDependencyInstructor.builder()
                                                      .key("TEST")
                                                      .providerPlanNodeId("ID")
                                                      .outcomeExpression("expression")
                                                      .build();

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testSupports() {
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("TEST").build();
    ExecutionPlanCreationContext context = ExecutionPlanCreationContextImpl.builder().build();
    boolean supports = instructor.supports(spec, context);
    assertThat(supports).isEqualTo(true);
    assertThat(instructor.supports(null, context)).isEqualTo(false);
    assertThat(instructor.getKey()).isEqualTo("TEST");
    assertThat(instructor.getProviderPlanNodeId()).isEqualTo("ID");
    assertThat(instructor.getOutcomeExpression()).isEqualTo("expression");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testAttachDependency() {
    PlanNodeBuilder builder = PlanNode.builder();
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("TEST").build();
    ExecutionPlanCreationContext context = ExecutionPlanCreationContextImpl.builder().build();
    instructor.attachDependency(spec, builder, context);
    PlanNode planNode = builder.build();
    List<RefObject> refObjects = planNode.getRefObjects();
    assertThat(refObjects.size()).isEqualTo(1);
    RefObject refObject = refObjects.get(0);
    assertThat(refObject.getKey()).isEqualTo("TEST");
    assertThat(refObject.getProducerId()).isEqualTo("ID");
    assertThat(refObject.getName()).isEqualTo("expression");
  }
}
