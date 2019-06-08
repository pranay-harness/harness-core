package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsAsgGetRunningCountData {
  private int asgMax;
  private String asgName;
}