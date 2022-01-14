package io.harness.cdng.buckets.resources.s3;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;

import java.util.Map;

@OwnedBy(CDP)
public interface S3ResourceService {
  Map<String, String> getBuckets(
      IdentifierRef awsConnectorRef, String region, String orgIdentifier, String projectIdentifier);
}
