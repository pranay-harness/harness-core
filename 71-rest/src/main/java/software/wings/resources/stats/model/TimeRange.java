package software.wings.resources.stats.model;

import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import javax.annotation.Nullable;

@Value
public class TimeRange {
  @Nullable private String label;

  // all timestamps in epoch millis
  private long from;
  private long to;

  @JsonCreator
  public TimeRange(@JsonProperty("from") long from, @JsonProperty("to") long to) {
    this(null, from, to);
  }

  @JsonCreator
  public TimeRange(
      @JsonProperty("label") @Nullable String label, @JsonProperty("from") long from, @JsonProperty("to") long to) {
    Preconditions.checkArgument(from < to, "Start Time should be strictly smaller than End Time");

    this.label = label;
    this.from = from;
    this.to = to;
  }

  public boolean isInRange(long tsInMillis) {
    return from < tsInMillis && tsInMillis < to;
  }
}
