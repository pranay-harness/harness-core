/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks;

import static io.harness.rule.OwnerRule.AMAN;
import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class LogSanitizerTest extends CategoryTest {
  private static final String TEST_VALUE = "testValue";
  private static final String TEST_KEY = "testKey";
  public static final String ACTIVITY_ID = "activityId";
  private static final String DIFFERENT_ACTIVITY_ID = "differentActivityId";

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testGenericSanitize_ShouldReplaceSecret() {
    HashMap<String, String> secrets = new HashMap<String, String>() {
      { put(TEST_KEY, TEST_VALUE); }
    };
    LogSanitizer logSanitizer = new GenericLogSanitizer(Sets.newHashSet(secrets.values()));

    String random = logSanitizer.sanitizeLog("random", "remove -> testValue <- ");
    assertThat(random.contains(TEST_VALUE)).isFalse();
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testSanitizeForActivityId_ShouldReplaceSecret() {
    HashMap<String, String> secrets = new HashMap<String, String>() {
      { put(TEST_KEY, TEST_VALUE); }
    };
    LogSanitizer logSanitizer = new ActivityBasedLogSanitizer(ACTIVITY_ID, Sets.newHashSet(secrets.values()));

    String random = logSanitizer.sanitizeLog(ACTIVITY_ID, "remove -> testValue <- ");
    assertThat(random.contains(TEST_VALUE)).isFalse();
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testSanitizeForActivityId_ShouldNotReplaceSecret() {
    HashMap<String, String> secrets = new HashMap<String, String>() {
      { put(TEST_KEY, TEST_VALUE); }
    };
    LogSanitizer logSanitizer = new ActivityBasedLogSanitizer(ACTIVITY_ID, Sets.newHashSet(secrets.values()));

    String random = logSanitizer.sanitizeLog(DIFFERENT_ACTIVITY_ID, "remove -> testValue <- ");
    assertThat(random.contains(TEST_VALUE)).isTrue();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCleanup() {
    assertThat(LogSanitizer.cleanup(null)).isNull();
    assertThat(LogSanitizer.cleanup("")).isNull();
    assertThat(LogSanitizer.cleanup("a")).isNull();
    assertThat(LogSanitizer.cleanup("ab")).isNull();
    assertThat(LogSanitizer.cleanup("  ab  ")).isNull();
    assertThat(LogSanitizer.cleanup("  ab cd  ")).isEqualTo("ab cd");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCalculateSecretLines() {
    assertThat(LogSanitizer.calculateSecretLines(ImmutableSet.<String>builder().add("").add(" a ").build())).isEmpty();
    assertThat(LogSanitizer.calculateSecretLines(ImmutableSet.<String>builder()
                                                     .add("this is multi-line \n"
                                                         + "secret\n"
                                                         + "\twith some white spaces")
                                                     .build()))
        .isEqualTo(ImmutableSet.<String>builder()
                       .add("this is multi-line")
                       .add("secret")
                       .add("with some white spaces")
                       .build());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testGenericSanitize_ShouldReplaceMultiLineSecret() {
    Set<String> secrets = ImmutableSet.<String>builder().add("line1\nline2\nline3").build();
    LogSanitizer logSanitizer = new GenericLogSanitizer(Sets.newHashSet(secrets));

    String result = logSanitizer.sanitizeLog("",
        "sanitize this log: line1\n"
            + "      line2\n"
            + "       line3");

    assertThat(result).isEqualTo("sanitize this log: **************\n"
        + "      **************\n"
        + "       **************");
  }
}
