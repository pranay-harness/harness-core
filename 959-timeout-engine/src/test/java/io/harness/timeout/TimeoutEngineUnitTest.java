package io.harness.timeout;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.TimeoutEngineTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTracker;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TimeoutEngineUnitTest extends TimeoutEngineTestBase {
  @Inject private TimeoutEngine timeoutEngine;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  @RealMongo
  public void testRegisterTimeout() {
    TestTimeoutCallback callback = new TestTimeoutCallback();
    TimeoutInstance instance = timeoutEngine.registerTimeout(new AbsoluteTimeoutTracker(1000), callback);
    timeoutEngine.handle(instance);
    assertThat(callback.getTimeoutInstance()).isNotNull();
  }

  public static class TestTimeoutCallback implements TimeoutCallback {
    private TimeoutInstance timeoutInstance;
    public TimeoutInstance getTimeoutInstance() {
      return timeoutInstance;
    }

    @Override
    public void onTimeout(TimeoutInstance timeoutInstance) {
      this.timeoutInstance = timeoutInstance;
    }
  }
}
