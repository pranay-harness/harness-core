package io.harness.connector.entities.embedded.awskmsconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AwsKmsManualCredentialKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.awskmsconnector.AwsKmsManualCredential")
public class AwsKmsManualCredential implements AwsKmsCredentialSpec {
  private String accessKey;
  private String secretKey;
}
