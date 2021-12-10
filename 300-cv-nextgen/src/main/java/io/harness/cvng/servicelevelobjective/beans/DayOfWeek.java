package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum DayOfWeek {
  @JsonProperty("Mon") MONDAY(0),
  @JsonProperty("Tue") TUESDAY(1),
  @JsonProperty("Wed") WEDNESDAY(2),
  @JsonProperty("Thu") THURSDAY(3),
  @JsonProperty("Fri") FRIDAY(4),
  @JsonProperty("Sat") SATURDAY(5),
  @JsonProperty("Sun") SUNDAY(6);
  int dayOfWeek;
  DayOfWeek(int dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public java.time.DayOfWeek getJavaDayOfWeek() {
    return java.time.DayOfWeek.of(this.dayOfWeek);
  }
  public LocalDate getNextDayOfWeek(LocalDate currentDate) {
    return currentDate.with(TemporalAdjusters.next(this.getJavaDayOfWeek()));
  }
}
