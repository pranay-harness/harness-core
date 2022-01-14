package io.harness.cvng.beans.customhealth;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TimestampInfo {
  String placeholder;
  TimestampFormat timestampFormat;
  String customTimestampFormat;

  public enum TimestampFormat { SECONDS, MILLISECONDS, CUSTOM }
}