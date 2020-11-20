package io.harness.executionplan.stepsdependency.resolvers;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.Outcome;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.refObjects.RefObjectUtil;
import io.harness.rule.Owner;
import io.harness.state.io.ResolvedRefInput;
import io.harness.state.io.StepTransput;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RefObjectStepDependencyResolverTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @InjectMocks RefObjectStepDependencyResolver resolver;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testResolve() {
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("SINGLE_KEY").build();
    StepTransput transput = new DummyOutcome("value");
    ResolvedRefInput input =
        ResolvedRefInput.builder().transput(transput).refObject(RefObjectUtil.getOutcomeRefObject("test")).build();
    Map<String, List<ResolvedRefInput>> refKeyToInputParamsMap = new HashMap<String, List<ResolvedRefInput>>() {
      {
        put("SINGLE_KEY", Collections.singletonList(input));
        put("MULTI_KEY", Arrays.asList(input, input));
      }
    };
    StepDependencyResolverContext resolverContext =
        StepDependencyResolverContext.defaultBuilder().refKeyToInputParamsMap(refKeyToInputParamsMap).build();

    Optional<DummyOutcome> resolve = resolver.resolve(spec, resolverContext);
    assertThat(resolve.isPresent()).isEqualTo(true);
    assertThat(resolve.get()).isEqualTo(transput);

    spec = StepDependencySpec.defaultBuilder().key("MULTI_KEY").build();
    Optional<List<DummyOutcome>> resolveList = resolver.resolve(spec, resolverContext);
    assertThat(resolveList.isPresent()).isEqualTo(true);
    assertThat(resolveList.get().size()).isEqualTo(2);
    assertThat(resolveList.get())
        .isEqualTo(refKeyToInputParamsMap.get("MULTI_KEY")
                       .stream()
                       .map(ResolvedRefInput::getTransput)
                       .collect(Collectors.toList()));

    resolve = resolver.resolve(null, resolverContext);
    assertThat(resolve.isPresent()).isEqualTo(false);
  }

  @Data
  @AllArgsConstructor
  public static class DummyOutcome implements Outcome {
    String name;
  }
}
