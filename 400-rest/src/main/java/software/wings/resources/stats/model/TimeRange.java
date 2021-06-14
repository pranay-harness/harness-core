package software.wings.resources.stats.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

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
@JsonIgnoreProperties({"inRange"})
@JsonInclude(Include.NON_NULL)
public class TimeRange {
  @Nullable private String label;

  // all timestamps in epoch millis
  private long from;
  private long to;
  private String timeZone;
  private boolean durationBased;
  private Long duration;

  @JsonCreator
  public TimeRange(@JsonProperty("from") long from, @JsonProperty("to") long to,
      @JsonProperty("timeZone") String timeZone, @JsonProperty("durationBased") boolean durationBased,
      @JsonProperty("duration") Long duration) {
    this(null, from, to, timeZone, durationBased, duration);
  }

  @JsonCreator
  public TimeRange(@JsonProperty("label") @Nullable String label, @JsonProperty("from") long from,
      @JsonProperty("to") long to, @JsonProperty("timeZone") String timeZone,
      @JsonProperty("durationBased") boolean durationBased, @JsonProperty("duration") Long duration) {
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
  }

  public boolean isInRange() {
    Calendar startCalendar = CalendarUtils.getCalendar(timeZone, from);
    Calendar endCalendar = CalendarUtils.getCalendar(timeZone, to);
    Calendar currentCalendar = CalendarUtils.getCalendarForTimeZone(timeZone);
    return startCalendar.before(currentCalendar) && currentCalendar.before(endCalendar);
  }

  @Data
  public static final class Yaml extends BaseYaml {
    private String from;
    private String to;
    private String timeZone;
    private boolean durationBased;
    private String duration;

    @Builder
    public Yaml(@JsonProperty("from") String from, @JsonProperty("to") String to,
        @JsonProperty("timeZone") String timeZone, @JsonProperty("durationBased") boolean durationBased,
        @JsonProperty("duration") String duration) {
      setFrom(from);
      setTo(to);
      setTimeZone(timeZone);
      setDurationBased(durationBased);
      setDuration(duration);
    }
  }
}
