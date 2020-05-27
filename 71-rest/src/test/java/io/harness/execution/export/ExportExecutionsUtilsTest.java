package io.harness.execution.export;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.ZonedDateTime;

public class ExportExecutionsUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testUploadFile() {
    assertThat(ExportExecutionsUtils.prepareZonedDateTime(0)).isNull();

    Instant now = Instant.now();
    ZonedDateTime zonedDateTime = ExportExecutionsUtils.prepareZonedDateTime(now.toEpochMilli());
    assertThat(zonedDateTime).isNotNull();
    assertThat(zonedDateTime.toInstant()).isEqualTo(now);
  }
}
