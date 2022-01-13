/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.utils;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.DataFormatException;
import io.harness.rule.Owner;

import com.google.protobuf.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HTimestampsTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testRoundTripToInstant() {
    Instant now = Instant.now();
    assertThat(HTimestamps.toInstant(HTimestamps.fromInstant(now))).isEqualTo(now);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testRoundTripToDate() {
    Date now = new Date(System.currentTimeMillis());
    assertThat(HTimestamps.toDate(HTimestamps.fromDate(now))).isEqualTo(now);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testRoundTripToMillis() {
    long currentMillis = Instant.now().toEpochMilli();
    assertThat(HTimestamps.toMillis(HTimestamps.fromMillis(currentMillis))).isEqualTo(currentMillis);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testParseDate() {
    String dateString = "2019-08-21T09:17:52.342Z";
    Timestamp timestamp = HTimestamps.parse(dateString);
    assertThat(timestamp.getSeconds()).isEqualTo(1_566_379_072L);
    assertThat(timestamp.getNanos()).isEqualTo(342_000_000L);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testParseErrorThrowsWingsException() {
    String dateString = "2019/08/21T09:17:52.342Z";
    assertThatExceptionOfType(DataFormatException.class)
        .isThrownBy(() -> HTimestamps.parse(dateString))
        .withMessage("Unparseable timestamp")
        .withCauseInstanceOf(ParseException.class);
  }
}
