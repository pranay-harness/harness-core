package io.harness.executionplan.stepsdependency.service;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.executionplan.stepsdependency.StepDependencyInstructor;
import io.harness.executionplan.stepsdependency.StepDependencyResolver;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.plan.PlanNode;
import io.harness.plan.PlanNode.PlanNodeBuilder;
import io.harness.rule.Owner;

import com.google.inject.name.Named;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class StepDependencyServiceImplTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock @Named("RefObjectResolver") StepDependencyResolver refObjectResolver;
  @Mock @Named("ExpressionResolver") StepDependencyResolver expressionResolver;
  @Mock StepDependencyInstructor instructor1;
  @Mock StepDependencyInstructor instructor2;

  @InjectMocks StepDependencyServiceImpl dependencyService;

  ExecutionPlanCreationContext context = ExecutionPlanCreationContextImpl.builder().build();

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testAttachDependency() {
    dependencyService.registerStepDependencyInstructor(instructor1, context);
    dependencyService.registerStepDependencyInstructor(instructor2, context);

    PlanNodeBuilder builder = PlanNode.builder();
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("TEST").build();
    doReturn(true).when(instructor1).supports(spec, context);
    dependencyService.attachDependency(spec, builder, context);
    verify(instructor1, times(1)).attachDependency(spec, builder, context);

    StepDependencySpec spec1 = StepDependencySpec.defaultBuilder().key("TEST3").build();
    assertThatThrownBy(() -> dependencyService.attachDependency(spec1, builder, context))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testResolve() {
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("TEST").build();
    StepDependencyResolverContext resolverContext = StepDependencyResolverContext.defaultBuilder().build();
    doReturn(Optional.empty()).when(refObjectResolver).resolve(spec, resolverContext);
    doReturn(Optional.of("RESULT")).when(expressionResolver).resolve(spec, resolverContext);
    Optional<String> resolve = dependencyService.resolve(spec, resolverContext);
    assertThat(resolve.isPresent()).isEqualTo(true);
    assertThat(resolve.get()).isEqualTo("RESULT");
  }
}
