package io.harness.remote;

import io.harness.secret.ConfigSecret;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class CEAwsSetupConfig {
  @ConfigSecret String accessKey;
  @ConfigSecret String secretKey;
  String destinationBucket;
  String templateURL;
}
