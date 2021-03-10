package io.harness.pms.core;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.PipelineServiceConfiguration;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import io.swagger.annotations.ApiOperation;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SwaggerApiOperationTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testNickNameUniqueness() {
    // Not adding PATCH at present.
    Set<Class<? extends Annotation>> supportedAnnotation = new HashSet<>();
    Collections.addAll(supportedAnnotation, GET.class, POST.class, PUT.class, DELETE.class);

    final Set<String> uniqueOperationName = Sets.newHashSet();

    final Class<? extends Annotation> apiOperationClass = ApiOperation.class;

    Collection<Class<?>> resourceClasses = PipelineServiceConfiguration.getResourceClasses();
    for (Class<?> clazz : resourceClasses) {
      for (final Method method : clazz.getDeclaredMethods()) {
        supportedAnnotation.stream().filter(method::isAnnotationPresent).forEach(annotation -> {
          assertThat(method.isAnnotationPresent(apiOperationClass)).isTrue();
          ApiOperation apiOperationAnnotation = (ApiOperation) method.getAnnotation(apiOperationClass);
          assertThat(apiOperationAnnotation.nickname()).isNotBlank();
          assertThat(uniqueOperationName.contains(apiOperationAnnotation.nickname())).isFalse();
          uniqueOperationName.add(apiOperationAnnotation.nickname());
        });
      }
    }
  }
}
