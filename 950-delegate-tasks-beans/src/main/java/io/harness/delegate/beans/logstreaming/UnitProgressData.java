package io.harness.delegate.beans.logstreaming;

import io.harness.delegate.beans.DelegateProgressData;
import io.harness.logging.UnitProgress;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UnitProgressData implements DelegateProgressData {
  List<UnitProgress> unitProgresses;
}
