package io.harness.connector.entities.embedded.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.amazonaws.auth.STSSessionCredentialsProvider;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Value
@Builder
@FieldNameConstants(innerTypeName = "AwsSecretManagerSTSCredentialKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.awssecretmanager.AwsSecretManagerSTSCredential")
public class AwsSecretManagerSTSCredential implements AwsSecretManagerCredentialSpec {
  String roleArn;
  String externalId;
  @Builder.Default int assumeStsRoleDuration = STSSessionCredentialsProvider.DEFAULT_DURATION_SECONDS;
  ;
}