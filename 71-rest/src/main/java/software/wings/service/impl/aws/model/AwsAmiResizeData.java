package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsAmiResizeData {
  private String asgName;
  private int desiredCount;
}