package io.harness.threading;

import io.harness.exception.CyclicBarrierException;
import org.junit.Test;

public class ConcurrentTest {
  @Test(expected = CyclicBarrierException.class)
  public void testExceptionPropagation() {
    Concurrent.test(1, i -> { throw new CyclicBarrierException(null); });
  }
}
