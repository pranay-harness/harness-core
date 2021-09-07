package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class AwsAsgGetRunningCountData {
  private int asgMin;
  private int asgMax;
  private int asgDesired;
  private String asgName;
}
