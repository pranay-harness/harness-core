package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public class AwsSecretManagerConstants {
  // credential type
  public static final String ASSUME_IAM_ROLE = "AssumeIAMRole";
  public static final String ASSUME_STS_ROLE = "AssumeSTSRole";
  public static final String MANUAL_CONFIG = "ManualConfig";
}
