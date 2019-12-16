package software.wings.resources.stats.model;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.time.CalendarUtils;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import javax.annotation.Nullable;

@Value
@Slf4j
@JsonIgnoreProperties({"inRange"})
public class TimeRange {
  @Nullable private String label;

  // all timestamps in epoch millis
  private long from;
  private long to;
  private String timeZone;

  @JsonCreator
  public TimeRange(
      @JsonProperty("from") long from, @JsonProperty("to") long to, @JsonProperty("timeZone") String timeZone) {
    this(null, from, to, timeZone);
  }

  @JsonCreator
  public TimeRange(@JsonProperty("label") @Nullable String label, @JsonProperty("from") long from,
      @JsonProperty("to") long to, @JsonProperty("timeZone") String timeZone) {
    Preconditions.checkArgument(from < to, "Start Time should be strictly smaller than End Time");

    this.label = label;
    this.from = from;
    this.to = to;
    this.timeZone = timeZone;
  }

  public boolean isInRange() {
    Calendar startCalendar = CalendarUtils.getCalendar(timeZone, from);
    Calendar endCalendar = CalendarUtils.getCalendar(timeZone, to);
    Calendar currentCalendar = CalendarUtils.getCalendarForTimeZone(timeZone);
    return startCalendar.before(currentCalendar) && currentCalendar.before(endCalendar);
  }
}
