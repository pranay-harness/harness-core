package io.harness.executionplan.stepsdependency.instructors;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.impl.CreateExecutionPlanContextImpl;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.plan.PlanNode;
import io.harness.plan.PlanNode.PlanNodeBuilder;
import io.harness.references.OutcomeRefObject;
import io.harness.references.RefObject;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

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
    CreateExecutionPlanContext context = CreateExecutionPlanContextImpl.builder().build();
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
    CreateExecutionPlanContext context = CreateExecutionPlanContextImpl.builder().build();
    instructor.attachDependency(spec, builder, context);
    PlanNode planNode = builder.build();
    List<RefObject> refObjects = planNode.getRefObjects();
    assertThat(refObjects.size()).isEqualTo(1);
    RefObject refObject = refObjects.get(0);
    assertThat(refObject).isInstanceOf(OutcomeRefObject.class);
    OutcomeRefObject outcomeRefObject = (OutcomeRefObject) refObject;
    assertThat(outcomeRefObject.getKey()).isEqualTo("TEST");
    assertThat(outcomeRefObject.getProducerId()).isEqualTo("ID");
    assertThat(outcomeRefObject.getName()).isEqualTo("expression");
  }
}