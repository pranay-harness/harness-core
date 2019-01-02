package software.wings.service.impl;

import io.harness.serializer.KryoUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceChangeEvent;

public class InstanceChangeEventListenerTest extends WingsBaseTest {
  @Test
  public void shouldClone() {
    KryoUtils.clone(InstanceChangeEvent.builder().build());
  }
}
