package io.harness.delegate.task.spotinst.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotinstElastigroupRunningCountData {
  private int elastigroupMin;
  private int elastigroupMax;
  private int elastigroupTarget;
  private String elastigroupName;
}
