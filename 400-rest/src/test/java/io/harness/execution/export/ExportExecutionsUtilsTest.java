/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.execution.export;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.time.Instant;
import java.time.ZonedDateTime;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
