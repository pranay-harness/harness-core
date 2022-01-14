package io.harness.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDP)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AwsConfig {
  boolean isEc2IamCredentials;
  boolean isIRSA;
  CrossAccountAccess crossAccountAccess;
  AwsAccessKeyCredential awsAccessKeyCredential;
}
