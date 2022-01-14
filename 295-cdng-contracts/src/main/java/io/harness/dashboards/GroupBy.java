package io.harness.dashboards;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.concurrent.TimeUnit;

@OwnedBy(PIPELINE)
public enum GroupBy {
  DAY("day", TimeUnit.DAYS.toMillis(1)),
  WEEK("week", TimeUnit.DAYS.toMillis(7)),
  MONTH("month", TimeUnit.DAYS.toMillis(30));

  private final String datePart;
  private final long noOfMilliseconds;

  GroupBy(String datePart, long noOfMilliseconds) {
    this.datePart = datePart;
    this.noOfMilliseconds = noOfMilliseconds;
  }

  public String getDatePart() {
    return datePart;
  }

  public long getNoOfMilliseconds() {
    return noOfMilliseconds;
  }
}
