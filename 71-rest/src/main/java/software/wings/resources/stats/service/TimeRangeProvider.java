package software.wings.resources.stats.service;

import lombok.NoArgsConstructor;
import software.wings.resources.stats.model.TimeRange;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.annotation.ParametersAreNonnullByDefault;

@NoArgsConstructor
@ParametersAreNonnullByDefault
public class TimeRangeProvider {
  private static final ZoneOffset DEFAULT_ZONE = ZoneOffset.UTC;

  private ZoneOffset zone = DEFAULT_ZONE;
  public TimeRangeProvider(ZoneOffset zone) {
    this.zone = zone;
  }

  public List<TimeRange> monthlyRanges(Instant from, Instant to) {
    List<TimeRange> ranges = new LinkedList<>();
    LocalDateTime fromTime = LocalDateTime.ofInstant(from, zone);
    LocalDateTime toTime = LocalDateTime.ofInstant(to, zone);

    TimeRange range = month(fromTime);
    ranges.add(range);

    LocalDateTime time = fromTime.with(TemporalAdjusters.firstDayOfNextMonth());
    while (!time.isAfter(toTime)) {
      ranges.add(month(time));
      time = time.with(TemporalAdjusters.firstDayOfNextMonth());
    }

    return ranges;
  }

  TimeRange month(LocalDateTime time) {
    Month month = Month.from(time);
    Year year = Year.from(time);

    LocalDateTime startOfMonth = time.with(TemporalAdjusters.firstDayOfMonth());
    LocalDateTime endOfMonth = time.with(TemporalAdjusters.lastDayOfMonth());

    String label = month.getDisplayName(TextStyle.FULL, Locale.CANADA) + " " + year.getValue();
    return new TimeRange(label, startOfMonth.toInstant(zone).toEpochMilli(), endOfMonth.toInstant(zone).toEpochMilli());
  }
}
