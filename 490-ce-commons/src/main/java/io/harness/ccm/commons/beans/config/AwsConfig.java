package io.harness.ccm.commons.beans.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsConfig {
  private String harnessAwsAccountId;
  private String awsConnectorTemplate;
}
