package io.harness.enforcement.bases;

import io.harness.enforcement.beans.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DurationRestriction extends Restriction {
  private TimeUnit timeUnit;
}
