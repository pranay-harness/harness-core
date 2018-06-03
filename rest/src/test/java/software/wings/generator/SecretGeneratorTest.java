package software.wings.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;

public class SecretGeneratorTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(SecretGeneratorTest.class);

  @Inject SecretGenerator secretGenerator;

  @Test
  public void testEncoding() {
    if (!secretGenerator.isInitialized()) {
      return;
    }

    final byte[] bytes = "this is a test of the cipher".getBytes();
    String test = secretGenerator.encrypt(bytes);
    logger.info(test);
    assertThat(secretGenerator.decrypt(test)).isEqualTo(bytes);
  }
}
