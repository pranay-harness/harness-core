package software.wings.resources.stats.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import javax.annotation.Nullable;

@Value
@AllArgsConstructor
public class TimeRange {
  @Nullable private String label;
  private long from;
  private long to;

  public TimeRange(long from, long to) {
    this.from = from;
    this.to = to;
    this.label = null;
  }
}
