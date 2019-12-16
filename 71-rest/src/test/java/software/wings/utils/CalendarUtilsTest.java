package software.wings.utils;

import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
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

  //  @Test
  //  @Owner(developers = VARDAN_BANSAL)
  //  @Category(UnitTests.class)
  //  public void testGetCalendar() {
  //    Calendar calendar = CalendarUtils.getCalendar(1, "1:00 AM", "Asia/Kolkata");
  //    assertThat(calendar.get(Calendar.DAY_OF_WEEK)).isEqualTo(5);
  //    assertThat(calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(1);
  //    assertThat(calendar.getTimeZone().getID()).isEqualTo("Asia/Kolkata");
  //  }
}
