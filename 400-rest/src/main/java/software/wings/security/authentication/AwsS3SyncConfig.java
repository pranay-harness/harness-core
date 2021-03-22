package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
@OwnedBy(CDP)
public class AwsS3SyncConfig {
  private String awsS3BucketName;
  private String awsAccessKey;
  private String awsSecretKey;
  private String region;
}
