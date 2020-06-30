package io.harness;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import io.harness.adviser.AdviserParameters;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.exception.UnexpectedException;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.references.RefObject;
import io.harness.reflection.CodeUtils;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepTransput;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ExcludeRedesign
@UtilityClass
@Slf4j
public class OrchestrationAliasUtils {
  private static final Set<Class<?>> BASE_ORCHESTRATION_INTERFACES =
      ImmutableSet.of(StepParameters.class, StepTransput.class, RefObject.class, AdviserParameters.class,
          FacilitatorParameters.class, ExecutableResponse.class, PassThroughData.class);

  public static void validateModule() {
    Map<String, Class<?>> allElements = new HashMap<>();
    Reflections reflections = new Reflections("io.harness.serializer.spring");
    try {
      for (Class clazz : reflections.getSubTypesOf(OrchestrationBeansAliasRegistrar.class)) {
        Constructor<?> constructor = null;
        constructor = clazz.getConstructor();
        final OrchestrationBeansAliasRegistrar aliasRegistrar =
            (OrchestrationBeansAliasRegistrar) constructor.newInstance();

        if (CodeUtils.isTestClass(clazz)) {
          continue;
        }
        logger.info("Checking registrar {}", clazz.getName());
        Map<String, Class<?>> orchestrationElements = new HashMap<>();
        aliasRegistrar.register(orchestrationElements);

        CodeUtils.checkHarnessClassesBelongToModule(
            CodeUtils.location(aliasRegistrar.getClass()), new HashSet<>(orchestrationElements.values()));

        Set<String> intersection = Sets.intersection(allElements.keySet(), orchestrationElements.keySet());
        if (isNotEmpty(intersection)) {
          throw new IllegalStateException("Aliases already registered. Please register with a new Alias: "
              + HarnessStringUtils.join("|", intersection));
        }
        orchestrationElements.forEach((k, v) -> {
          if (isBaseInterfaceAssignable(v)) {
            allElements.put(k, v);
          }
        });
      }
      validateBaseEntityRegistrations(allElements);
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new UnexpectedException("Unexpected exception while constructing registrar", e);
    }
  }

  private static void validateBaseEntityRegistrations(Map<String, Class<?>> allElements) {
    Reflections reflections = new Reflections("io.harness", "software.wings");
    Set<Class<?>> implementationClasses = new HashSet<>();
    BASE_ORCHESTRATION_INTERFACES.forEach(clazz
        -> implementationClasses.addAll(reflections.getSubTypesOf(clazz)
                                            .stream()
                                            .filter(cl -> !cl.isInterface() && !CodeUtils.isTestClass(cl))
                                            .collect(Collectors.toSet())));
    Set<Class<?>> allElementsClassSet = new HashSet<>(allElements.values());
    if (implementationClasses.size() != allElementsClassSet.size()) {
      Set<Class<?>> diff = allElementsClassSet.size() > implementationClasses.size()
          ? Sets.difference(allElementsClassSet, implementationClasses)
          : Sets.difference(implementationClasses, allElementsClassSet);
      throw new IllegalStateException("Not all classes registered "
          + HarnessStringUtils.join("|", diff.stream().map(Class::getSimpleName).collect(Collectors.toSet())));
    }
  }

  private static boolean isBaseInterfaceAssignable(Class<?> clazz) {
    for (Class<?> interfaceClass : BASE_ORCHESTRATION_INTERFACES) {
      if (interfaceClass.isAssignableFrom(clazz)) {
        return true;
      }
    }
    return false;
  }
}
