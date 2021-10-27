package io.harness.cf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ConfigSecret;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CF)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class CfMigrationConfig {
  private boolean enabled;
  private String adminUrl;
  @ConfigSecret private String apiKey;
  private String account;
  private String org;
  private String project;
  private String environment;
  @Default private int connectionTimeout = 10000;
  @Default private int readTimeout = 10000;
}
