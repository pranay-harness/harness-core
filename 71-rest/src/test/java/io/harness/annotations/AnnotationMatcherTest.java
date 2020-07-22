package io.harness.annotations;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Preconditions;

import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.reflection.CodeUtils;
import io.harness.rule.Owner;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;
import software.wings.WingsBaseTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationMatcherTest extends WingsBaseTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testAllAnnotatedClasses() throws IOException {
    Set<Class<? extends Object>> redesignClasses = new HashSet<>();
    Reflections reflectionsHarness = new Reflections("io.harness");
    Reflections reflectionsWings = new Reflections("software.wings");
    redesignClasses.addAll(reflectionsHarness.getTypesAnnotatedWith(ExcludeRedesign.class));
    redesignClasses.addAll(reflectionsWings.getTypesAnnotatedWith(ExcludeRedesign.class));
    List<String> nameList = redesignClasses.stream()
                                .distinct()
                                .filter(clazz -> !clazz.isAnonymousClass() && !clazz.isMemberClass())
                                .map(Class::getName)
                                .sorted()
                                .collect(Collectors.toList());
    List<String> expectedList;
    try (InputStream in = getClass().getResourceAsStream("/annotations/redesign-class-list.txt")) {
      expectedList = IOUtils.readLines(in, "UTF-8");
    }
    assertThat(expectedList).containsAll(nameList);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testAllCoverageClasses() throws IOException {
    Set<Class<? extends Object>> redesignClasses = new HashSet<>();
    Reflections reflectionsHarness = new Reflections("io.harness");
    Reflections reflectionsWings = new Reflections("software.wings");
    redesignClasses.addAll(reflectionsHarness.getTypesAnnotatedWith(OwnedBy.class));
    redesignClasses.addAll(reflectionsWings.getTypesAnnotatedWith(OwnedBy.class));
    Set<String> nameSet =
        redesignClasses.stream()
            .distinct()
            .filter(clazz -> !clazz.isAnonymousClass() && !clazz.isMemberClass() && !clazz.isInterface())
            .filter(clazz
                -> clazz.getAnnotation(OwnedBy.class) != null
                    && clazz.getAnnotation(OwnedBy.class).value().equals(HarnessTeam.CDC))
            .map(this ::extractClassLocation)
            .collect(Collectors.toSet());
    Set<String> expectedSet;
    try (InputStream in = getClass().getResourceAsStream("/annotations/coverage-class-list.txt")) {
      expectedSet = new HashSet<>(IOUtils.readLines(in, "UTF-8"));
    }
    assertThat(nameSet).isSubsetOf(expectedSet);
    assertThat(expectedSet).isSubsetOf(nameSet);
  }

  private String extractClassLocation(Class<?> clazz) {
    List<String> locationList = Arrays.asList(Preconditions.checkNotNull(CodeUtils.location(clazz)).split("/"));
    String moduleBasePath = locationList.get(locationList.indexOf("target") - 1) + "/src/main/java/";
    String packagePath = clazz.getPackage().getName().replace(".", "/") + "/";
    return moduleBasePath + packagePath + clazz.getSimpleName() + ".java";
  }
}
