package software.wings.utils;

import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.time.CalendarUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Calendar;

public class CalendarUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void testGetCalendarForTimeZone() {
    Calendar calendar = CalendarUtils.getCalendarForTimeZone("Asia/Kolkata");
    assertThat(calendar.getTimeZone().getID()).isEqualTo("Asia/Kolkata");
  }
}
