package io.harness.grpc.utils;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import io.harness.exception.DataFormatException;
import lombok.experimental.UtilityClass;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Utility class to handle mapping between {@link Timestamp} and java timestamp types.
 */
@UtilityClass
@ParametersAreNonnullByDefault
public class HTimestamps {
  public Timestamp fromInstant(Instant instant) {
    return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
  }

  public Instant toInstant(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }

  public Timestamp fromMillis(long epoch) {
    return fromInstant(Instant.ofEpochMilli(epoch));
  }

  public long toMillis(Timestamp timestamp) {
    return toInstant(timestamp).toEpochMilli();
  }

  public Timestamp fromDate(Date date) {
    return fromInstant(date.toInstant());
  }

  public Date toDate(Timestamp timestamp) {
    return Date.from(toInstant(timestamp));
  }

  public Timestamp parse(String time) {
    try {
      return Timestamps.parse(time);
    } catch (ParseException e) {
      throw new DataFormatException("Unparseable timestamp", e);
    }
  }
}
