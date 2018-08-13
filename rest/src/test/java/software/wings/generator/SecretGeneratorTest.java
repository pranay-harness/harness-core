package software.wings.generator;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.resource.Project;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.generator.SecretGenerator.SecretName;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
  public void rebuildSecretProperties() throws URISyntaxException, IOException {
    if (!secretGenerator.isInitialized()) {
      return;
    }

    final Integer length = secretGenerator.getSecrets()
                               .keySet()
                               .stream()
                               .map(value -> ((String) value).length())
                               .max(Integer::compare)
                               .get();

    final String pattern = format("%%-%ds = %%s%%n", length);

    final List<Object> sortedKeys = secretGenerator.getSecrets().keySet().stream().sorted().collect(toList());

    final Path secretsPath =
        Paths.get(Project.rootDirectory(), "rest", "src", "test", "resources", "secrets.properties");

    String passphrase = System.getenv("NEW_HARNESS_GENERATION_PASSPHRASE");
    if (passphrase == null) {
      passphrase = SecretGenerator.passphrase;
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(secretsPath.toFile()))) {
      for (Object key : sortedKeys) {
        final SecretName secretName = SecretName.builder().value((String) key).build();
        final String secret =
            SecretGenerator.encrypt(secretGenerator.decryptToString(secretName).getBytes(), passphrase);

        final String line = format(pattern, key, secret);
        if (line.length() <= 120) {
          writer.write(line);
          continue;
        }

        String multiLineSecret = StringUtils.EMPTY;
        for (int i = 0; i < secret.length(); i += 118) {
          multiLineSecret += format("\\%n%s", secret.substring(i, Math.min(i + 118, secret.length())));
        }
        final String multiLine = format(pattern, key, multiLineSecret);
        writer.write(multiLine);
        writer.write(format("%n"));
      }
    }
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
