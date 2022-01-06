/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.annotation;

import static io.harness.rule.OwnerRule.PIYUSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.retry.IMethodWrapper;
import io.harness.annotations.retry.MethodExecutionHelper;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;

public class RetryOnExceptionAnnotationTest extends CategoryTest {
  private MethodExecutionHelper methodExecutionHelper = new MethodExecutionHelper();

  private IMethodWrapper<Object> methodWrapper;

  @Before
  public void setUp() throws Exception {
    methodWrapper = Mockito.mock(MockMethodWrapperImpl.class);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH)
  @Category(UnitTests.class)
  public void test_IfRetryIsAttemptedForConfiguredTimesInCaseOfException() throws Exception {
    Mockito.when(methodWrapper.execute()).thenThrow(SQLException.class);
    try {
      methodExecutionHelper.execute(methodWrapper, 2, 10, SQLException.class);
    } catch (Exception exception) {
      assertThat(exception.getClass().isAssignableFrom(SQLException.class)).isTrue();
    }
    Collection<Invocation> invocations = Mockito.mockingDetails(methodWrapper).getInvocations();
    assertThat(invocations).hasSize(2);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH)
  @Category(UnitTests.class)
  public void test_IfRetryIsAttemptedOnlyForConfiguredException() throws Exception {
    Mockito.when(methodWrapper.execute()).thenThrow(NullPointerException.class);
    try {
      methodExecutionHelper.execute(methodWrapper, 2, 10, SQLException.class);
    } catch (Exception exception) {
      assertThat(exception.getClass().isAssignableFrom(NullPointerException.class)).isTrue();
    }
    Collection<Invocation> invocations = Mockito.mockingDetails(methodWrapper).getInvocations();
    assertThat(invocations).hasSize(1);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH)
  @Category(UnitTests.class)
  public void test_IfRetriesAreAttemptedByConfiguredDelay() throws Exception {
    Mockito.when(methodWrapper.execute()).thenThrow(SQLException.class);
    int numberOfRetries = 5;
    int delayBetweenRetries = 1000;
    int expectedDelayInMilliSeconds = (numberOfRetries - 1) * delayBetweenRetries;
    long startTime = System.nanoTime();
    try {
      methodExecutionHelper.execute(methodWrapper, numberOfRetries, delayBetweenRetries, SQLException.class);
    } catch (Exception exception) {
      long actualDelayInMilliSeconds = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
      assertThat(actualDelayInMilliSeconds >= expectedDelayInMilliSeconds).isTrue();
      assertThat(exception.getClass().isAssignableFrom(SQLException.class)).isTrue();
    }
  }
}
