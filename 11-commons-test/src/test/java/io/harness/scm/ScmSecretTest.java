package io.harness.scm;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Inject;

import com.bettercloud.vault.VaultException;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.resource.Project;
import io.harness.rule.CommonsMethodRule;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class ScmSecretTest extends CategoryTest {
  @Rule public CommonsMethodRule commonsMethodRule = new CommonsMethodRule();
  @Inject ScmSecret scmSecret;

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testVault() throws VaultException {
    if (!scmSecret.isInitialized()) {
      return;
    }
    assertThat(scmSecret.obtain("/datagen/!!!test", "do-not-delete")).isEqualTo("hello");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testDecrypt() {
    if (!scmSecret.isInitialized()) {
      return;
    }
    assertThat(scmSecret.decryptToString(new SecretName("aws_playground_access_key"))).isNotEmpty();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testEncoding() {
    if (!scmSecret.isInitialized()) {
      return;
    }

    String text = "udo6OkOATXXGFZR/J0RCUDrtib6njyGbZtCv2c+v";

    final byte[] bytes = text.getBytes(UTF_8);
    String test = scmSecret.encrypt(bytes);

    for (int i = 0; i < test.length() / 118; i++) {
      logger.info(test.substring(i * 118, (i + 1) * 118) + "\\");
    }

    int skip = (test.length() / 118) * 118;
    if (test.length() - skip != 0) {
      logger.info(test.substring(skip));
    }

    logger.info(test);
    assertThat(scmSecret.decrypt(test)).isEqualTo(bytes);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @Ignore("Bypass this test, it is not for running regularly")
  public void rebuildSecretProperties() throws URISyntaxException, IOException {
    assertThatCode(() -> {
      if (!scmSecret.isInitialized()) {
        return;
      }

      final Integer length = scmSecret.getSecrets()
                                 .keySet()
                                 .stream()
                                 .map(value -> ((String) value).length())
                                 .max(Integer::compare)
                                 .orElseGet(null);

      final String pattern = format("%%-%ds = %%s%%n", length);

      final List<Object> sortedKeys = scmSecret.getSecrets().keySet().stream().sorted().collect(toList());

      final Path secretsPath = Paths.get(
          Project.rootDirectory(getClass()), "11-commons-test", "src", "main", "resources", "secrets.properties");

      String passphrase = System.getenv("NEW_HARNESS_GENERATION_PASSPHRASE");
      if (passphrase == null) {
        passphrase = ScmSecret.passphrase;
      }
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(secretsPath.toFile()))) {
        for (Object key : sortedKeys) {
          final SecretName secretName = SecretName.builder().value((String) key).build();
          final byte[] decrypt = scmSecret.decrypt(secretName);

          // logger.info("{} = {}", secretName, new String(decrypt));

          final String secret = ScmSecret.encrypt(decrypt, passphrase);

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
    })
        .doesNotThrowAnyException();
  }
}
