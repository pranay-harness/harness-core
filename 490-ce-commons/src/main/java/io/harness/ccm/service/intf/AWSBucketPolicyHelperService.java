package io.harness.ccm.service.intf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CE)
public interface AWSBucketPolicyHelperService {
  boolean updateBucketPolicy(String crossAccountRoleArn, String awsS3Bucket, String awsAccessKey, String awsSecretKey);
}
