package io.harness.executionplan.stepsdependency.resolvers;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.pms.ambiance.Ambiance;
import io.harness.rule.Owner;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ExpressionStepDependencyResolverTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock EngineExpressionService engineExpressionService;
  @InjectMocks ExpressionStepDependencyResolver resolver;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testResolve() {
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("TEST").build();
    Ambiance ambiance = Ambiance.newBuilder().build();
    StepDependencyResolverContext resolverContext =
        StepDependencyResolverContext.defaultBuilder().ambiance(ambiance).build();

    doReturn("RESULT").when(engineExpressionService).evaluateExpression(ambiance, "TEST");

    Optional<String> resolve = resolver.resolve(spec, resolverContext);
    assertThat(resolve.isPresent()).isEqualTo(true);
    assertThat(resolve.get()).isEqualTo("RESULT");

    resolve = resolver.resolve(null, resolverContext);
    assertThat(resolve.isPresent()).isEqualTo(false);
  }
}
