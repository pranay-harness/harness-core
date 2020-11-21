package io.harness.orchestration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.exception.UnexpectedException;
import io.harness.reflection.CodeUtils;
import io.harness.spring.AliasRegistrar;

import com.google.common.collect.Sets;
import com.google.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class OrchestrationAliasUtils {
  public static void validateModule(Provider<Set<Class<? extends AliasRegistrar>>> providerClasses) {
    Map<String, Class<?>> allElements = new HashMap<>();
    try {
      for (Class<? extends AliasRegistrar> clazz : providerClasses.get()) {
        Constructor<?> constructor = null;
        constructor = clazz.getConstructor();
        final AliasRegistrar aliasRegistrar = (AliasRegistrar) constructor.newInstance();
        if (CodeUtils.isTestClass(clazz)) {
          continue;
        }
        log.info("Checking registrar {}", clazz.getName());
        Map<String, Class<?>> orchestrationElements = new HashMap<>();
        aliasRegistrar.register(orchestrationElements);

        CodeUtils.checkHarnessClassesBelongToModule(
            CodeUtils.location(aliasRegistrar.getClass()), new HashSet<>(orchestrationElements.values()));

        Set<String> intersection = Sets.intersection(allElements.keySet(), orchestrationElements.keySet());
        if (isNotEmpty(intersection)) {
          throw new IllegalStateException("Aliases already registered. Please register with a new Alias: "
              + HarnessStringUtils.join("|", intersection));
        }
      }
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new UnexpectedException("Unexpected exception while constructing registrar", e);
    }
  }
}
