package io.harness.logging;

import static io.harness.logging.LoggingInitializer.RESPONSE_MESSAGE_FILE;
import static org.junit.Assert.assertEquals;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class ErrorCodePropertiesTest {
  @Test
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
    assertEquals(errorCodeSet, propertiesSet);
  }
}