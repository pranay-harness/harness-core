package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsS3BucketDetails {
  private String region;
  private String s3BucketName;
}
