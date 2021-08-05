package io.harness.aws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

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
