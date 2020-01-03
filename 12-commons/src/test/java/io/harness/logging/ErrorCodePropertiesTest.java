package io.harness.logging;

import static io.harness.logging.LoggingInitializer.RESPONSE_MESSAGE_FILE;
import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class ErrorCodePropertiesTest extends CategoryTest {
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testErrorCodesInProperties() {
    Properties messages = new Properties();
    try (InputStream in = getClass().getResourceAsStream(RESPONSE_MESSAGE_FILE)) {
      messages.load(in);
    } catch (IOException exception) {
      throw new WingsException(exception);
    }

    Set<String> errorCodeSet =
        Arrays.stream(ErrorCode.values()).map(error -> error.toString()).collect(Collectors.toSet());
    Set<String> propertiesSet =
        messages.keySet().stream().map(message -> message.toString()).collect(Collectors.toSet());

    // Assert that all errorCodes are defined in properties
    // and each property should have ErrorCode enum
    assertThat(propertiesSet).isEqualTo(errorCodeSet);
  }
}