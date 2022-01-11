/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.logging;

import static io.harness.logging.LoggingInitializer.RESPONSE_MESSAGE_FILE;
import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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

    Set<String> errorCodeSet = Arrays.stream(ErrorCode.values()).map(Enum::toString).collect(Collectors.toSet());
    Set<String> propertiesSet = messages.keySet().stream().map(Object::toString).collect(Collectors.toSet());

    // Assert that all errorCodes are defined in properties
    // and each property should have ErrorCode enum
    assertThat(propertiesSet).isEqualTo(errorCodeSet);
  }
}
