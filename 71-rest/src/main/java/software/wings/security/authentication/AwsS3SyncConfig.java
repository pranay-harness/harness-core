package software.wings.security.authentication;

import com.google.inject.Singleton;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class AwsS3SyncConfig {
  private String awsAccessKey;
  private String awsSecretKey;
  private String region;
}
