package io.harness.event.handler.segment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secret.ConfigSecret;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ujjawal on 12/18/19
 */
@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class SalesforceConfig {
  @ConfigSecret String userName;
  @ConfigSecret String password;
  @ConfigSecret String consumerKey;
  @ConfigSecret String consumerSecret;
  String grantType;
  String loginInstanceDomain;
  String apiVersion;
  boolean enabled;
}
