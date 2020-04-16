package io.harness.annotations;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;
import software.wings.WingsBaseTest;

import java.io.IOException;
import java.io.InputStream;
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
    redesignClasses.addAll(reflectionsHarness.getTypesAnnotatedWith(Redesign.class));
    redesignClasses.addAll(reflectionsWings.getTypesAnnotatedWith(Redesign.class));
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
    assertThat(expectedList).isEqualTo(nameList);
  }
}
