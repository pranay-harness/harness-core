package io.harness.annotation;

import io.harness.annotations.retry.IMethodWrapper;
import io.harness.annotations.retry.MethodExecutionHelper;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;

public class RetryOnExceptionAnnotationTest {
  private MethodExecutionHelper methodExecutionHelper = new MethodExecutionHelper();

  private IMethodWrapper<Object> methodWrapper;

  @Before
  public void setUp() throws Exception {
    methodWrapper = Mockito.mock(MockMethodWrapperImpl.class);
  }

  @SneakyThrows
  @Test
  public void test_IfRetryIsAttemptedForConfiguredTimesInCaseOfException() throws Exception {
    Mockito.when(methodWrapper.execute()).thenThrow(SQLException.class);
    try {
      methodExecutionHelper.execute(methodWrapper, 2, 10, SQLException.class);
    } catch (Exception exception) {
      Assert.assertTrue(exception.getClass().isAssignableFrom(SQLException.class));
    }
    Collection<Invocation> invocations = Mockito.mockingDetails(methodWrapper).getInvocations();
    int numberOfCalls = invocations.size();
    Assert.assertEquals(2, numberOfCalls);
  }

  @SneakyThrows
  @Test
  public void test_IfRetryIsAttemptedOnlyForConfiguredException() throws Exception {
    Mockito.when(methodWrapper.execute()).thenThrow(NullPointerException.class);
    try {
      methodExecutionHelper.execute(methodWrapper, 2, 10, SQLException.class);
    } catch (Exception exception) {
      Assert.assertTrue(exception.getClass().isAssignableFrom(NullPointerException.class));
    }
    Collection<Invocation> invocations = Mockito.mockingDetails(methodWrapper).getInvocations();
    int numberOfCalls = invocations.size();
    Assert.assertEquals(1, numberOfCalls);
  }

  @SneakyThrows
  @Test
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
      Assert.assertTrue(actualDelayInMilliSeconds >= expectedDelayInMilliSeconds);
      Assert.assertTrue(exception.getClass().isAssignableFrom(SQLException.class));
    }
  }
}
