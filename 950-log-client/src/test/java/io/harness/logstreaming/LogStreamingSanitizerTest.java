/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logstreaming;

import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.rule.OwnerRule.MARKO;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.HashSet;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LogStreamingSanitizerTest extends CategoryTest {
  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldNotSanitizeWhenNoSecretsAvailable() {
    String message = "test message without secrets";
    io.harness.logstreaming.LogLine logLine = io.harness.logstreaming.LogLine.builder().message(message).build();

    LogStreamingSanitizer logStreamingSanitizer = LogStreamingSanitizer.builder().secrets(null).build();

    logStreamingSanitizer.sanitizeLogMessage(logLine);
    Assertions.assertThat(logLine.getMessage()).isEqualTo(message);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldSanitizeWhenSecretsAreAvailable() {
    String message = "test message with secret1 and secret2";
    String sanitizedMessage = "test message with " + SECRET_MASK + " and " + SECRET_MASK;

    io.harness.logstreaming.LogLine logLine = LogLine.builder().message(message).build();

    Set<String> secrets = new HashSet<>();
    secrets.add("secret1");
    secrets.add("secret2");

    io.harness.logstreaming.LogStreamingSanitizer logStreamingSanitizer =
        LogStreamingSanitizer.builder().secrets(secrets).build();

    logStreamingSanitizer.sanitizeLogMessage(logLine);
    Assertions.assertThat(logLine.getMessage()).isEqualTo(sanitizedMessage);
  }
}
