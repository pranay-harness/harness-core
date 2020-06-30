package io.harness;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
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
import io.harness.spring.AliasRegistrar;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepTransput;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    Map<String, Class<?>> allBaseInterfaceElements = new HashMap<>();
    Reflections reflections = new Reflections("io.harness.serializer.spring");
    try {
      for (Class clazz : reflections.getSubTypesOf(AliasRegistrar.class)) {
        Constructor<?> constructor = null;
        constructor = clazz.getConstructor();
        final AliasRegistrar aliasRegistrar = (AliasRegistrar) constructor.newInstance();

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
        allElements.putAll(orchestrationElements);
        orchestrationElements.forEach((k, v) -> {
          if (isBaseInterfaceAssignable(v)) {
            allBaseInterfaceElements.put(k, v);
          }
        });
      }
      validateBaseEntityRegistrations(allBaseInterfaceElements, allElements);
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new UnexpectedException("Unexpected exception while constructing registrar", e);
    }
  }

  private static void validateBaseEntityRegistrations(
      Map<String, Class<?>> allBaseInterfaceElements, Map<String, Class<?>> allElements) {
    Reflections reflections = new Reflections("io.harness", "software.wings");
    Set<Class<?>> implementationClasses = new HashSet<>();
    BASE_ORCHESTRATION_INTERFACES.forEach(clazz
        -> implementationClasses.addAll(reflections.getSubTypesOf(clazz)
                                            .stream()
                                            .filter(cl -> !cl.isInterface() && !CodeUtils.isTestClass(cl))
                                            .collect(Collectors.toSet())));
    Set<Class<?>> allElementsClassSet = new HashSet<>(allBaseInterfaceElements.values());
    if (implementationClasses.size() != allElementsClassSet.size()) {
      Set<Class<?>> diff = allElementsClassSet.size() > implementationClasses.size()
          ? Sets.difference(allElementsClassSet, implementationClasses)
          : Sets.difference(implementationClasses, allElementsClassSet);
      throw new IllegalStateException("Not all classes registered "
          + HarnessStringUtils.join("|", diff.stream().map(Class::getSimpleName).collect(Collectors.toSet())));
    }
    Set<Class<?>> exceptionClasses =
        validateNestedBaseEntityFields(implementationClasses, new HashSet<>(allElements.values()));
    if (!isEmpty(exceptionClasses)) {
      throw new IllegalStateException("Not all classes registered "
          + HarnessStringUtils.join(
                "|", exceptionClasses.stream().map(Class::getSimpleName).collect(Collectors.toSet())));
    }
  }

  private static Set<Class<?>> validateNestedBaseEntityFields(
      Set<Class<?>> implementationClasses, Set<Class<?>> registeredClass) {
    Set<Class<?>> exceptionClasses = new HashSet<>();
    for (Class<?> implementationClass : implementationClasses) {
      exceptionClasses.addAll(checkAllRegistered(implementationClass, registeredClass));
    }
    return exceptionClasses;
  }

  private static Set<Class<?>> checkAllRegistered(Class<?> targetClass, Set<Class<?>> registeredClass) {
    Set<Class<?>> fieldClasses = new HashSet<>();
    Set<Class<?>> exceptionClasses = new HashSet<>();
    for (Field field : targetClass.getDeclaredFields()) {
      Class<?> fieldClass = field.getType();
      if (field.getGenericType() instanceof ParameterizedType) {
        Set<Class<?>> parameterizedClasses = new HashSet<>();
        Type[] classTypes = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
        for (Type type : classTypes) {
          if (type instanceof Class && !ignoreCheck((Class<?>) type)) {
            parameterizedClasses.add((Class<?>) type);
          }
        }
        if (isNotEmpty(parameterizedClasses)) {
          exceptionClasses.addAll(validateNestedBaseEntityFields(parameterizedClasses, registeredClass));
        }
      }
      if (ignoreCheck(fieldClass)) {
        continue;
      }
      fieldClasses.add(fieldClass);
      if (!registeredClass.contains(fieldClass)) {
        exceptionClasses.add(fieldClass);
      }
    }

    if (isEmpty(fieldClasses)) {
      return fieldClasses;
    }
    exceptionClasses.addAll(validateNestedBaseEntityFields(fieldClasses, registeredClass));
    return exceptionClasses;
  }

  private static boolean ignoreCheck(Class<?> fieldClass) {
    return !CodeUtils.isHarnessClass(fieldClass) || fieldClass.isInterface() || CodeUtils.isTestClass(fieldClass)
        || fieldClass.isEnum();
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
