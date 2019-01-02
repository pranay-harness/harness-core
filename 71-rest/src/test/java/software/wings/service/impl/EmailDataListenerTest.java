package software.wings.service.impl;

import io.harness.serializer.KryoUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.helpers.ext.mail.EmailData;

public class EmailDataListenerTest extends WingsBaseTest {
  @Test
  public void shouldClone() {
    KryoUtils.clone(EmailData.builder().build());
  }
}
