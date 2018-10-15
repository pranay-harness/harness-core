package io.harness.scm;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.resource.Project;
import io.harness.rule.CommonsMethodRule;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ScmSecretTest {
  private static final Logger logger = LoggerFactory.getLogger(ScmSecretTest.class);

  @Rule public CommonsMethodRule commonsMethodRule = new CommonsMethodRule();
  @Inject ScmSecret scmSecret;

  @Test
  public void testEncoding() {
    if (!scmSecret.isInitialized()) {
      return;
    }

    String text = "this is a test of the cipher";

    final byte[] bytes = text.getBytes();
    String test = scmSecret.encrypt(bytes);

    for (int i = 0; i < test.length() / 118; i++) {
      logger.info(test.substring(i * 118, (i + 1) * 118) + "\\");
    }

    int skip = (test.length() / 118) * 118;
    if (test.length() - skip != 0) {
      logger.info(test.substring(skip, test.length()));
    }

    logger.info(test.substring(skip, test.length()));

    logger.info(test);
    assertThat(scmSecret.decrypt(test)).isEqualTo(bytes);
  }

  @Test
  public void rebuildSecretProperties() throws URISyntaxException, IOException {
    if (!scmSecret.isInitialized()) {
      return;
    }

    final Integer length =
        scmSecret.getSecrets().keySet().stream().map(value -> ((String) value).length()).max(Integer::compare).get();

    final String pattern = format("%%-%ds = %%s%%n", length);

    final List<Object> sortedKeys = scmSecret.getSecrets().keySet().stream().sorted().collect(toList());

    final Path secretsPath =
        Paths.get(Project.rootDirectory(), "11-commons-test", "src", "main", "resources", "secrets.properties");

    String passphrase = System.getenv("NEW_HARNESS_GENERATION_PASSPHRASE");
    if (passphrase == null) {
      passphrase = ScmSecret.passphrase;
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(secretsPath.toFile()))) {
      for (Object key : sortedKeys) {
        final SecretName secretName = SecretName.builder().value((String) key).build();
        final String secret = ScmSecret.encrypt(scmSecret.decryptToString(secretName).getBytes(), passphrase);

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
}