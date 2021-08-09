package io.harness.ccm.commons.entities;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsAccountConnectionDetail {
  private String externalId;
  private String harnessAccountId;
  private String cloudFormationTemplateLink;
  private String stackLaunchTemplateLink;
}
