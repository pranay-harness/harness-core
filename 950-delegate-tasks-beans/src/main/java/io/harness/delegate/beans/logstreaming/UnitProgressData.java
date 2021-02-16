package io.harness.delegate.beans.logstreaming;

import io.harness.logging.UnitProgress;
import io.harness.tasks.ProgressData;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UnitProgressData implements ProgressData {
  List<UnitProgress> unitProgresses;
}
