package io.harness.cf;

import io.harness.secret.ConfigSecret;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class CfClientConfig {
  @ConfigSecret private String apiKey;
  @Default private String configUrl = "https://config.feature-flags.uat.harness.io/api/1.0";
  @Default private String eventUrl = "https://event.feature-flags.uat.harness.io/api/1.0";
  private boolean analyticsEnabled;
  @Default private int connectionTimeout = 10000;
  @Default private int readTimeout = 10000;
}