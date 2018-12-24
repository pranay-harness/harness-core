package software.wings.service.impl.instance.licensing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InstanceUsageLimitCheckerImplTest {
  @Test
  public void testIsWithinLimit() {
    double actualUsage = 90;
    double allowed = 100;
    boolean withinLimit = InstanceUsageLimitCheckerImpl.isWithinLimit(actualUsage, 85, allowed);
    assertFalse(withinLimit);
    withinLimit = InstanceUsageLimitCheckerImpl.isWithinLimit(actualUsage, 95, allowed);
    assertTrue(withinLimit);
  }
}