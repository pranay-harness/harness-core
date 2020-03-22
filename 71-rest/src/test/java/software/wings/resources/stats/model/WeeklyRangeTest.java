package software.wings.resources.stats.model;

import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.governance.WeeklyFreezeConfig;
import io.harness.rule.Owner;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.governance.GovernanceConfig;

import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

public class WeeklyRangeTest extends CategoryTest {
  private String accountId = "some-account-uuid-" + RandomStringUtils.randomAlphanumeric(5);

  /**
   * Deployments should not be allowed if current point in time is range of a freeze window
   */
  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  @Ignore("Will fix it with feature enhancement")
  public void testIsInRange() {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
    WeeklyRange weeklyRange = new WeeklyRange(null, "Monday", "2:00 AM", "Monday", "1:00 AM", "Asia/Kolkata");
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
    if (dayOfWeek == 2 && hourOfDay == 1) {
      assertThat(weeklyRange.isInRange()).isEqualTo(true);
    } else {
      assertThat(weeklyRange.isInRange()).isEqualTo(false);
    }
  }

  /**
   * Should not be able to add a weekly window with invalid range
   */
  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void createGovernanceConfig_shouldThrowException() {
    try {
      TimeRange range = new TimeRange(100L, 200L, "Asia/Kolkata");
      WeeklyRange weeklyRange = new WeeklyRange(null, "Monday", "7:00 PM", "Tuesday", "5:00 AM", "Asia/Kolkata");
      TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig = new TimeRangeBasedFreezeConfig(
          true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD), range);
      WeeklyFreezeConfig weeklyFreezeConfig = new WeeklyFreezeConfig(
          true, Collections.emptyList(), Collections.singletonList(EnvironmentType.PROD), weeklyRange);

      GovernanceConfig.builder()
          .accountId(accountId)
          .deploymentFreeze(false)
          .timeRangeBasedFreezeConfigs(Collections.singletonList(timeRangeBasedFreezeConfig))
          .weeklyFreezeConfigs(Collections.singletonList(weeklyFreezeConfig))
          .build();
    } catch (Exception ex) {
      assertTrue(ex instanceof IllegalArgumentException);
    }
  }
}
