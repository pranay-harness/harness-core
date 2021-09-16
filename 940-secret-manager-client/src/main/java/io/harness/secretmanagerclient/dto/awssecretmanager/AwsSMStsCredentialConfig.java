package io.harness.secretmanagerclient.dto.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.amazonaws.auth.STSSessionCredentialsProvider;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Getter
@Setter
@Builder
@FieldNameConstants(innerTypeName = "AwsSMStsCredentialConfigKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.secretmanagerclient.dto.awssecretmanager.AwsSMStsCredentialConfig")
public class AwsSMStsCredentialConfig implements AwsSMCredentialSpecConfig {
  String roleArn;
  String externalId;
  @Builder.Default int assumeStsRoleDuration = STSSessionCredentialsProvider.DEFAULT_DURATION_SECONDS;
}