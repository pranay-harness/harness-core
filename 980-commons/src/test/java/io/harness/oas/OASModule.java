package io.harness.oas;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.testing.TestExecution;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

public abstract class OASModule extends AbstractModule {
  public abstract Collection<Class<?>> getResourceClasses();

  public void testOASAdoption(Collection<Class<?>> classes) {
    if (classes == null) {
      return;
    }
    for (Class<?> clazz : classes) {
      if (clazz.isAnnotationPresent(Tag.class)) {
        for (final Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isPublic(method.getModifiers()) && method.isAnnotationPresent(Operation.class)) {
            if (!checkIfHiddenApi(method)) {
              checkParamAnnotation(method);
            }
          }
        }
      }
    }
  }

  private boolean checkIfHiddenApi(Method method) {
    Annotation[] methodAnnotations = method.getDeclaredAnnotations();
    for (Annotation annotation : methodAnnotations) {
      if (annotation.annotationType() == Operation.class) {
        Operation operation = (Operation) annotation;
        return operation.hidden();
      }
    }
    return false;
  }

  private void checkParamAnnotation(Method method) {
    Annotation[][] parametersAnnotationsList = method.getParameterAnnotations();
    for (Annotation[] annotations : parametersAnnotationsList) {
      for (Annotation parameterAnnotation : annotations) {
        if (parameterAnnotation.annotationType() == Parameter.class) {
          Parameter parameter = (Parameter) parameterAnnotation;
          assertThat(parameter.description()).isNotEmpty();
        }
      }
    }
  }

  @Override
  protected void configure() {
    MapBinder<String, TestExecution> testExecutionMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
    testExecutionMapBinder.addBinding("OAS test").toInstance(() -> testOASAdoption(getResourceClasses()));
  }
}
