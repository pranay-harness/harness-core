package io.harness.threading;

import static io.harness.rule.OwnerRule.UNKNOWN;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.CyclicBarrierException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConcurrentTest extends CategoryTest {
  @Test(expected = CyclicBarrierException.class)
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testExceptionPropagation() {
    Concurrent.test(1, i -> { throw new CyclicBarrierException(null); });
  }
}
