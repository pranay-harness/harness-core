package io.harness.waiter;

import io.harness.tasks.ProgressData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StringNotifyProgressData implements ProgressData {
  String data;
}
