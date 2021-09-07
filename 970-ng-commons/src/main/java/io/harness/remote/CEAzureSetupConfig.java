package io.harness.remote;
import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class CEAzureSetupConfig {
  private String azureAppClientId;
  private String azureAppClientSecret;
}
