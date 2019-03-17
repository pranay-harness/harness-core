package io.harness.threading;

import io.harness.category.element.UnitTests;
import io.harness.exception.CyclicBarrierException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConcurrentTest {
  @Test(expected = CyclicBarrierException.class)
  @Category(UnitTests.class)
  public void testExceptionPropagation() {
    Concurrent.test(1, i -> { throw new CyclicBarrierException(null); });
  }
}
