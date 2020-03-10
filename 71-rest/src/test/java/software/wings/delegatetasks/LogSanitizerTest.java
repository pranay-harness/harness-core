package software.wings.delegatetasks;

import static io.harness.rule.OwnerRule.AMAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;

public class LogSanitizerTest {
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
    LogSanitizer logSanitizer = new LogSanitizer(LogSanitizer.GENERIC_ACTIVITY_ID, Sets.newHashSet(secrets.values()));

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
    LogSanitizer logSanitizer = new LogSanitizer(ACTIVITY_ID, Sets.newHashSet(secrets.values()));

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
    LogSanitizer logSanitizer = new LogSanitizer(ACTIVITY_ID, Sets.newHashSet(secrets.values()));

    String random = logSanitizer.sanitizeLog(DIFFERENT_ACTIVITY_ID, "remove -> testValue <- ");
    assertThat(random.contains(TEST_VALUE)).isTrue();
  }
}