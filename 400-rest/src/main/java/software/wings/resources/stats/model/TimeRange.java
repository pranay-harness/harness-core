/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.stats.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.governance.TimeRangeOccurrence;
import io.harness.time.CalendarUtils;
import io.harness.yaml.BaseYaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import java.util.Calendar;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Slf4j
@JsonIgnoreProperties(value = {"inRange"})
@JsonInclude(Include.NON_NULL)
@OwnedBy(HarnessTeam.CDC)
public class TimeRange {
  @Nullable private String label;

  // all timestamps in epoch millis
  private long from;
  private long to;
  private String timeZone;
  private boolean durationBased;
  private Long duration;
  private Long endTime;
  private TimeRangeOccurrence freezeOccurrence;
  // this field is used for proper rendering in UI
  private boolean expires;

  public TimeRange(@JsonProperty("from") long from, @JsonProperty("to") long to,
      @JsonProperty("timeZone") String timeZone, @JsonProperty("durationBased") boolean durationBased,
      @JsonProperty("duration") Long duration, @JsonProperty("endTime") Long endTime,
      @JsonProperty("freezeOccurrence") TimeRangeOccurrence freezeOccurrence,
      @JsonProperty("expires") boolean expires) {
    this(null, from, to, timeZone, durationBased, duration, endTime, freezeOccurrence, expires);
  }

  @JsonCreator
  public TimeRange(@JsonProperty("label") @Nullable String label, @JsonProperty("from") long from,
      @JsonProperty("to") long to, @JsonProperty("timeZone") String timeZone,
      @JsonProperty("durationBased") boolean durationBased, @JsonProperty("duration") Long duration,
      @JsonProperty("endTime") Long endTime, @JsonProperty("freezeOccurrence") TimeRangeOccurrence freezeOccurrence,
      @JsonProperty("expires") boolean expires) {
    if (durationBased) {
      Preconditions.checkArgument(
          duration != null && duration >= 1800000, "Duration might not be less than one 30 min");
    } else {
      Preconditions.checkArgument(from < to, "Start Time should be strictly smaller than End Time");
    }

    this.label = label;
    this.from = from;
    this.to = durationBased ? from + duration : to;
    this.timeZone = timeZone;
    this.durationBased = durationBased;
    this.duration = duration;
    this.endTime = endTime;
    this.freezeOccurrence = freezeOccurrence;
    this.expires = expires;
  }

  public boolean isInRange() {
    if (freezeOccurrence != null) {
      return freezeOccurrence.getTimeRangeChecker().istTimeInRange(this, System.currentTimeMillis());
    }
    Calendar startCalendar = CalendarUtils.getCalendar(timeZone, from);
    Calendar endCalendar = CalendarUtils.getCalendar(timeZone, to);
    Calendar currentCalendar = CalendarUtils.getCalendarForTimeZone(timeZone);
    return startCalendar.before(currentCalendar) && currentCalendar.before(endCalendar);
  }

  public TimeRange.Yaml toYaml() {
    TimeRange.Yaml.YamlBuilder timeRangeYamlBuilder = TimeRange.Yaml.builder().from(String.valueOf(this.getFrom()));
    boolean durationBased = this.isDurationBased();
    if (durationBased) {
      long duration = this.getTo() - this.getFrom();
      timeRangeYamlBuilder.duration(String.valueOf(duration));
      timeRangeYamlBuilder.durationBased(durationBased);
    }
    Yaml yaml = timeRangeYamlBuilder.to(String.valueOf(this.getTo())).build();
    if (endTime != null && freezeOccurrence != null) {
      yaml.setEndTime(String.valueOf(this.getEndTime()));
      yaml.setFreezeOccurrence(this.getFreezeOccurrence().name());
    }
    if (EmptyPredicate.isNotEmpty(this.getTimeZone())) {
      yaml.setTimeZone(this.getTimeZone());
    }
    return yaml;
  }

  @Data
  public static final class Yaml extends BaseYaml {
    private String from;
    private String to;
    private String timeZone;
    private boolean durationBased;
    private String duration;
    private String freezeOccurrence;
    private String endTime;

    @Builder
    public Yaml(@JsonProperty("from") String from, @JsonProperty("to") String to,
        @JsonProperty("timeZone") String timeZone, @JsonProperty("durationBased") boolean durationBased,
        @JsonProperty("duration") String duration, @JsonProperty("freezeOccurrence") String freezeOccurrence,
        @JsonProperty("endTime") String endTime) {
      setFrom(from);
      setTo(to);
      setTimeZone(timeZone);
      setDurationBased(durationBased);
      setDuration(duration);
      setFreezeOccurrence(freezeOccurrence);
      setEndTime(endTime);
    }
  }
}
