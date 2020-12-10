package software.wings.service.impl.aws.model;

import io.harness.pms.sdk.core.data.SweepingOutput;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsEcsAllPhaseRollbackData implements SweepingOutput {
  private boolean allPhaseRollbackDone;
}