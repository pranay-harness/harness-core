package software.wings.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.generator.SecretGenerator.SecretName;

public class SecretGeneratorTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(SecretGeneratorTest.class);

  @Inject SecretGenerator secretGenerator;

  @Test
  public void testEncoding() {
    if (!secretGenerator.isInitialized()) {
      return;
    }

    String text = "this is a test of the cipher";

    final byte[] bytes = text.getBytes();
    String test = secretGenerator.encrypt(bytes);

    for (int i = 0; i < test.length() / 118; i++) {
      logger.info(test.substring(i * 118, (i + 1) * 118) + "\\");
    }

    int skip = (test.length() / 118) * 118;
    if (test.length() - skip != 0) {
      logger.info(test.substring(skip, test.length()));
    }

    logger.info(test.substring(skip, test.length()));

    logger.info(test);
    assertThat(secretGenerator.decrypt(test)).isEqualTo(bytes);
  }

  @Test
  public void testStore() {
    if (!secretGenerator.isInitialized()) {
      return;
    }

    final SecretName secretKey = SecretName.builder().value("secret_key").build();

    String id1 = secretGenerator.ensureStored(GLOBAL_ACCOUNT_ID, secretKey);
    String id2 = secretGenerator.ensureStored(GLOBAL_ACCOUNT_ID, secretKey);
    assertThat(id1).isEqualTo(id2);
  }
}
