package io.harness.grpc.utils;

import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;

import io.harness.exception.DataFormatException;
import lombok.experimental.UtilityClass;

import java.text.ParseException;

@UtilityClass
public class HDurations {
  // To suppress checked exception
  public Duration parse(String duration) {
    try {
      return Durations.parse(duration);
    } catch (ParseException e) {
      throw new DataFormatException("Invalid duration string", e);
    }
  }
}
