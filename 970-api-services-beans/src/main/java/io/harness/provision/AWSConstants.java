package io.harness.provision;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public final class AWSConstants {
  private AWSConstants() {}
  public static final String AWS_SAM_WORKING_DIRECTORY = "./aws-sam-working-dir/";
  public static final String AWS_SAM_APP_REPOSITORY_DIR = "sam-app-repository";
}
