/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
