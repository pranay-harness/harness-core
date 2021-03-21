package io.harness.connector.entities.embedded.awscodecommitconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

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
@OwnedBy(DX)
public class AwsCodeCommitSecretKeyAccessKey implements AwsCodeCommitHttpsCredential {
  String secretKeyRef;
  String accessKey;
  String accessKeyRef;
}