package io.harness.remote;
import io.harness.secret.ConfigSecret;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class CEAzureSetupConfig {
  private String azureAppClientId;
  @ConfigSecret private String azureAppClientSecret;
}
