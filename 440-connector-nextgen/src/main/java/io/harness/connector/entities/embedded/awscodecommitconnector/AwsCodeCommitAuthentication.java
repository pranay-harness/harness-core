package io.harness.connector.entities.embedded.awscodecommitconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AwsCodeCommitAuthenticationKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.awscodecommitconnector.AwsCodeCommitAuthentication")
@OwnedBy(DX)
public class AwsCodeCommitAuthentication {
  AwsCodeCommitAuthType authType;
  AwsCodeCommitHttpsAuthType credentialsType;
  AwsCodeCommitHttpsCredential credential;
}
