package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LastRepositoryInfo {
  private long StartTime;
  private long EndTime;
  private String status;
  private String commit;
}
