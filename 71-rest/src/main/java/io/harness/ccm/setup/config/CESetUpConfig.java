package io.harness.ccm.setup.config;

import com.google.inject.Singleton;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class CESetUpConfig {
  private String awsAccountId;
  private String cloudFormationTemplateLink;
}
