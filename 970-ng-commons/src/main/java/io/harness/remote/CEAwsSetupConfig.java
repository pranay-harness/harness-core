package io.harness.remote;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class CEAwsSetupConfig {
  String accessKey;
  String secretKey;
  String destinationBucket;
}
