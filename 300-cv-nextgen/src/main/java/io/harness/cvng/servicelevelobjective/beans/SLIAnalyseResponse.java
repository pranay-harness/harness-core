package io.harness.cvng.servicelevelobjective.beans;

import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SLIAnalyseResponse {
  private Instant timeStamp;
  private SLIState sliState;
  private long runningBadCount;
  private long runningGoodCount;
}
