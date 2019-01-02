package software.wings.service.impl;

import io.harness.serializer.KryoUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.api.KmsTransitionEvent;

public class KmsTransitionEventListenerTest extends WingsBaseTest {
  @Test
  public void shouldClone() {
    KryoUtils.clone(KmsTransitionEvent.builder().build());
  }
}
