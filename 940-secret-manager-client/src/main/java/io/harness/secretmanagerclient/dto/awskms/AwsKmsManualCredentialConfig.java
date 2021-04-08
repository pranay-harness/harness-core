package io.harness.secretmanagerclient.dto.awskms;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Getter
@Setter
@Builder
@FieldNameConstants(innerTypeName = "AwsKmsManualCredentialConfigKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.secretmanagerclient.dto.awskms.AwsKmsManualCredentialConfig")
public class AwsKmsManualCredentialConfig implements AwsKmsCredentialSpecConfig {
  private String accessKey;
  private String secretKey;
}
