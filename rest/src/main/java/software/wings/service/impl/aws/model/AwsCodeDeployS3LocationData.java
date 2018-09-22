package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsCodeDeployS3LocationData {
  private String key;
  private String bucket;
  private String bundleType;
}