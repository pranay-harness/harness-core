package io.harness.cvng.core.beans;

import java.util.SortedSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StackdriverSampleDataDTO {
  SortedSet<TimeSeriesSampleDTO> sampleData;
  String errorMessage;
}
