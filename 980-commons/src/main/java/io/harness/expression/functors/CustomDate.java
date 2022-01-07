/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.expression.functors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.utils.DateTimeUtils;

import java.time.LocalDate;

@OwnedBy(HarnessTeam.CDC)
public class CustomDate {
  private LocalDate date;

  public CustomDate() {
    this.date = LocalDate.now();
  }

  public CustomDate plusYears(long years) {
    date = date.plusYears(years);
    return this;
  }

  public CustomDate plusMonths(long months) {
    date = date.plusMonths(months);
    return this;
  }

  public CustomDate plusWeeks(long weeks) {
    date = date.plusWeeks(weeks);
    return this;
  }

  public CustomDate plusDays(long days) {
    date = date.plusDays(days);
    return this;
  }

  @Override
  public String toString() {
    return DateTimeUtils.formatDate(date);
  }
}
