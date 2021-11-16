package io.harness.serializer.morphia;

import static io.harness.packages.HarnessPackages.IO_HARNESS;
import static io.harness.packages.HarnessPackages.SOFTWARE_WINGS;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

@Slf4j
public class KryoUtilTest extends WingsBaseTest {
  @Inject KryoSerializer kryoSerializer;

  Class<?> classToRunTest = GithubConnectorDTO.class;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testKryoSerializable() {
    Reflections reflection = new Reflections(IO_HARNESS, SOFTWARE_WINGS);
    Set<Class<?>> classesInvolved = new HashSet<>();
    getAllInvolvedClasses(classesInvolved, classToRunTest, reflection);
    Set<Class<?>> errorClasses = new HashSet<>();
    classesInvolved.forEach(clazz -> {
      if (!kryoSerializer.isRegistered(clazz) && !isAbstractOrInterface(clazz)) {
        log.error("Class [{}] not registered in kryo.", clazz);
        errorClasses.add(clazz);
      }
    });
    assertThat(errorClasses).isEmpty();
  }

  private void getAllInvolvedClasses(Set<Class<?>> classesInvolved, Class<?> clazz, Reflections reflections) {
    if (classesInvolved.contains(clazz)) {
      return;
    }
    // if class has subtypes traverse them.
    Set<Class<?>> subTypesOf = reflections.getSubTypesOf((Class<Object>) clazz);
    if (!subTypesOf.isEmpty()) {
      subTypesOf.forEach(subtype -> {
        if (checkIfClassShouldBeTraversed(subtype)) {
          getAllInvolvedClasses(classesInvolved, subtype, reflections);
        }
      });
    }

    // traverse all fields of that class.
    for (Field field : clazz.getDeclaredFields()) {
      field.setAccessible(true);
      final Class<?> type = field.getType();
      if (classesInvolved.contains(type)) {
        continue;
      }
      if (checkIfClassShouldBeTraversed(type)) {
        getAllInvolvedClasses(classesInvolved, type, reflections);
        classesInvolved.add(type);
      }
    }
    classesInvolved.add(clazz);
  }

  private boolean isAbstractOrInterface(Class<?> clazz) {
    return Modifier.isAbstract(clazz.getModifiers());
  }

  public boolean checkIfClassShouldBeTraversed(Class<?> clazz) {
    // Generating only for harness classes hence checking if package is software.wings or io.harness.
    return !clazz.isPrimitive() && !clazz.isEnum()
        && (clazz.getCanonicalName().startsWith("io.harness") || clazz.getCanonicalName().startsWith("software.wings"));
  }
}
