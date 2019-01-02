package software.wings.service.impl;

import io.harness.serializer.KryoUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;

public class DelayEventListenerTest extends WingsBaseTest {
  @Test
  public void shouldClone() {
    KryoUtils.clone(new DelayEvent("", null, null));
  }
}
