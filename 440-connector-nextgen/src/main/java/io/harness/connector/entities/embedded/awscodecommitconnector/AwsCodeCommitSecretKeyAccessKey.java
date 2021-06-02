package io.harness.connector.entities.embedded.awscodecommitconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AwsCodeCommitSecretKeyAccessKeyKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitSecretKeyAccessKey")
public class AwsCodeCommitSecretKeyAccessKey implements AwsCodeCommitHttpsCredential {
  String secretKeyRef;
  String accessKey;
  String accessKeyRef;
}