package io.harness.ccm.commons.beans.config;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CE)
public class AwsConfig {
  private String harnessAwsAccountId;
  private String awsConnectorTemplate;
  private String awsAccessKey;
  private String awsSecretKey;
}
