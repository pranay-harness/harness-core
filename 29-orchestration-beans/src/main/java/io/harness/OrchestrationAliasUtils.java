package io.harness;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Sets;

import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.exception.UnexpectedException;
import io.harness.reflection.CodeUtils;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ExcludeRedesign
@UtilityClass
@Slf4j
public class OrchestrationAliasUtils {
  public static void validateModule() {
    Map<String, Class<?>> allElements = new HashMap<>();
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
      }
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new UnexpectedException("Unexpected exception while constructing registrar", e);
    }
  }
}
