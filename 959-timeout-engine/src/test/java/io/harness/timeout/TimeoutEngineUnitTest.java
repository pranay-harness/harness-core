/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.timeout;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.TimeoutEngineTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTracker;

import com.google.inject.Inject;
import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class TimeoutEngineUnitTest extends TimeoutEngineTestBase {
  @Inject private TimeoutEngine timeoutEngine;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testRegisterTimeout() {
    TestTimeoutCallback callback = new TestTimeoutCallback();
    TimeoutInstance instance = timeoutEngine.registerTimeout(new AbsoluteTimeoutTracker(1000), callback);
    timeoutEngine.handle(instance);
    assertThat(callback.getTimeoutInstance()).isNotNull();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testRegisterAbsoluteTimeout() {
    TestTimeoutCallback callback = new TestTimeoutCallback();
    TimeoutInstance instance = timeoutEngine.registerAbsoluteTimeout(Duration.ofMillis(1000), callback);
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
