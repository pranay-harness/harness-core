/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oas;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.testing.TestExecution;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
public abstract class OASModule extends AbstractModule {
  public static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  public static final String ACCOUNT_ID = "accountId";
  public static final String ENDPOINT_VALIDATION_EXCLUSION_FILE = "/oas/exclude-endpoint-validation";

  public abstract Collection<Class<?>> getResourceClasses();

  public void testOASAdoption(Collection<Class<?>> classes) {
    List<String> endpointsWithoutAccountParam = new ArrayList<>();

    if (classes == null) {
      return;
    }
    for (Class<?> clazz : classes) {
      if (clazz.isAnnotationPresent(Tag.class)) {
        for (final Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isPublic(method.getModifiers()) && method.isAnnotationPresent(Operation.class)
              && !isHiddenApi(method)) {
            checkParamAnnotation(method);
            endpointsWithoutAccountParam.addAll(checkAccountIdentifierParam(method));
          }
        }
      }
    }

    if (!endpointsWithoutAccountParam.isEmpty()) {
      endpointsWithoutAccountParam.removeAll(excludedEndpoints());
      assertThat(endpointsWithoutAccountParam.isEmpty()).as(getDetails(endpointsWithoutAccountParam)).isTrue();
    }
  }

  private String getDetails(List<String> endpointsWithoutAccountParam) {
    StringBuilder details = new StringBuilder(
        "There should not be endpoints without account identifier as path OR query param, but found below : ");
    endpointsWithoutAccountParam.forEach(entry -> details.append("\n ").append(entry));
    return details.toString();
  }

  private List<String> excludedEndpoints() {
    List<String> excludedEndpoints = new ArrayList<>();
    try (InputStream in = getClass().getResourceAsStream(ENDPOINT_VALIDATION_EXCLUSION_FILE)) {
      if (in == null) {
        log.info("No endpoint exclusion configured for OAS validations.");
        return excludedEndpoints;
      }
      excludedEndpoints = IOUtils.readLines(in, "UTF-8");
    } catch (Exception e) {
      log.error("Failed to load endpoint exclusion file {} with error: {}", ENDPOINT_VALIDATION_EXCLUSION_FILE,
          e.getMessage());
    }
    return excludedEndpoints;
  }

  private boolean isHiddenApi(Method method) {
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

  private List<String> checkAccountIdentifierParam(Method method) {
    List<String> endpointsWithoutAccountParam = new ArrayList<>();
    boolean hasAccountIdentifierQueryParam = false;
    boolean hasAccountIdentifierPathParam = false;

    Annotation[][] parametersAnnotationsList = method.getParameterAnnotations();
    for (Annotation[] annotations : parametersAnnotationsList) {
      if (hasAccountIdentifierQueryParam || hasAccountIdentifierPathParam) {
        break;
      }
      for (Annotation parameterAnnotation : annotations) {
        if (parameterAnnotation.annotationType() == QueryParam.class) {
          QueryParam queryParameter = (QueryParam) parameterAnnotation;
          hasAccountIdentifierQueryParam =
              ACCOUNT_IDENTIFIER.equals(queryParameter.value()) || ACCOUNT_ID.equals(queryParameter.value());
        }
        if (parameterAnnotation.annotationType() == PathParam.class) {
          PathParam pathParameter = (PathParam) parameterAnnotation;
          hasAccountIdentifierPathParam =
              ACCOUNT_IDENTIFIER.equals(pathParameter.value()) || ACCOUNT_ID.equals(pathParameter.value());
        }
      }
    }
    if (!hasAccountIdentifierQueryParam && !hasAccountIdentifierPathParam) {
      endpointsWithoutAccountParam.add(method.getDeclaringClass().getName() + "." + method.getName());
    }
    return endpointsWithoutAccountParam;
  }

  @Override
  protected void configure() {
    MapBinder<String, TestExecution> testExecutionMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
    testExecutionMapBinder.addBinding("OAS test").toInstance(() -> testOASAdoption(getResourceClasses()));
  }
}
