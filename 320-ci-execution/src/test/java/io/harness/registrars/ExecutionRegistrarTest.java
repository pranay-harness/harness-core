package io.harness.registrars;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

@Slf4j
public class ExecutionRegistrarTest extends CIExecutionTest {
  @Inject private ExecutionRegistrar executionRegistrar;
  private final String STEP_PACKAGE = "io.harness.states";

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldRegisterAllCiStepSubtypes() {
    log.info("Checking if ExecutionRegistrar registers all Step<T> classes from '{}' package", STEP_PACKAGE);
    Set<Pair<StepType, Step>> stateClasses = new HashSet<>();
    executionRegistrar.register(stateClasses);
    Set<Class<? extends Step>> registeredClasses =
        stateClasses.stream().map(p -> p.getValue().getClass()).collect(Collectors.toSet());
    assertThat(stateClasses).isNotNull();

    Set<Class<? extends Step>> allClasses = getStepClasses(STEP_PACKAGE);
    boolean failTest = false;
    for (Class<? extends Step> clazz : allClasses) {
      boolean contains = registeredClasses.contains(clazz);
      if (!contains) {
        log.error("Register {} class with ExecutionRegistrar, add following line:", clazz.getSimpleName());
        log.error("  stateClasses.add(Pair.of({}.STEP_TYPE, injector.getInstance({}.class)));", clazz.getSimpleName(),
            clazz.getSimpleName());
        if (!failTest) {
          failTest = true;
        }
      }
    }
    assertThat(failTest).isFalse();
  }

  public static Set<Class<? extends Step>> getStepClasses(String prefix) {
    final Reflections reflections = new Reflections(prefix);
    final Set<Class<? extends Step>> payloadTypes = reflections.getSubTypesOf(Step.class);
    payloadTypes.removeIf(pc -> pc.isInterface() || Modifier.isAbstract(pc.getModifiers()));
    return payloadTypes;
  }
}