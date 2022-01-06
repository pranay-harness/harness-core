/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.git;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.secret.SecretSanitizerThreadLocal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

@Slf4j
@OwnedBy(PL)
public class ExceptionSanitizerTest extends CategoryTest {
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testSanitizationOfErrorMessage() {
    SecretSanitizerThreadLocal.set(Collections.singleton("password123"));
    final String singlePasswordMasked = ExceptionSanitizer.sanitizeTheMessage("The password is password123");
    assertThat(singlePasswordMasked).isEqualTo("The password is #######");
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testSanitizationOfRepeatedErrorMessage() {
    SecretSanitizerThreadLocal.set(Collections.singleton("password123"));
    final String singlePasswordMasked =
        ExceptionSanitizer.sanitizeTheMessage("The password is password123, password123");
    assertThat(singlePasswordMasked).isEqualTo("The password is #######, #######");
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testSanitizationOfMultipleSecrets() {
    SecretSanitizerThreadLocal.set(new HashSet<>(Arrays.asList("password456", "password123")));
    final String doublePasswordMasked =
        ExceptionSanitizer.sanitizeTheMessage("The password is password123, password456");
    assertThat(doublePasswordMasked).isEqualTo("The password is #######, #######");
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testSanitizationOfExceptionForLogging() {
    SecretSanitizerThreadLocal.set(Collections.singleton("password123"));
    final String singlePasswordMasked =
        ExceptionSanitizer.sanitizeForLogging(new Exception("This exception contains password: password123"));
    assertThat(singlePasswordMasked.contains("password123")).isFalse();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testSanitizationOfNestedExceptionForLogging() {
    SecretSanitizerThreadLocal.set(Collections.singleton("password123"));
    final String singlePasswordMasked = ExceptionSanitizer.sanitizeForLogging(
        new Exception("This exception contains password: password123", new Exception("Nested exception: password123")));
    assertThat(singlePasswordMasked.contains("password123")).isFalse();
  }
}
